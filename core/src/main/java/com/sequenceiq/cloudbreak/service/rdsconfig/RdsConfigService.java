package com.sequenceiq.cloudbreak.service.rdsconfig;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.RdsConfigRepository;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentResourceRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.AbstractEnvironmentAwareService;

@Service
public class RdsConfigService extends AbstractEnvironmentAwareService<RDSConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsConfigService.class);

    @Inject
    private RdsConfigRepository rdsConfigRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private RdsConnectionValidator rdsConnectionValidator;

    @Inject
    private TransactionService transactionService;

    @Override
    public RDSConfig attachToEnvironments(String resourceName, Set<String> environments, @NotNull Long workspaceId) {
        try {
            return transactionService.required(() -> super.attachToEnvironments(resourceName, environments, workspaceId));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    @Override
    public RDSConfig detachFromEnvironments(String resourceName, Set<String> environments, @NotNull Long workspaceId) {
        try {
            return transactionService.required(() -> super.detachFromEnvironments(resourceName, environments, workspaceId));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<RDSConfig> retrieveRdsConfigsInWorkspace(Workspace workspace) {
        return rdsConfigRepository.findAllByWorkspaceId(workspace.getId());
    }

    public RDSConfig getByNameForWorkspace(String name, Workspace workspace) {
        return getByNameForWorkspaceId(name, workspace.getId());
    }

    public RDSConfig get(Long id) {
        return rdsConfigRepository.findById(id).orElseThrow(notFound("RDS configuration", id));
    }

    public void delete(Long id) {
        RDSConfig rdsConfig = rdsConfigRepository.findById(id)
                .orElseThrow(notFound("RDS configuration", id));
        delete(rdsConfig);
    }

    public RDSConfig delete(String name) {
        RDSConfig rdsConfig = Optional.ofNullable(rdsConfigRepository.findUserManagedByName(name))
                .orElseThrow(notFound("RDS configuration", name));
        delete(rdsConfig);
        return rdsConfig;
    }

    public RDSConfig createIfNotExists(User user, RDSConfig rdsConfig, Long workspaceId) {
        Optional<RDSConfig> configByName = rdsConfigRepository.findByNameAndWorkspaceId(rdsConfig.getName(), workspaceId);
        if (configByName.isEmpty()) {
            Workspace workspace = getWorkspaceService().get(workspaceId, user);
            return create(rdsConfig, workspace, user);
        }
        return rdsConfig;

    }

    public Set<RDSConfig> findByClusterId(Long clusterId) {
        return rdsConfigRepository.findByClusterId(clusterId);
    }

    public RDSConfig findByClusterIdAndType(Long clusterId, DatabaseType databaseType) {
        return rdsConfigRepository.findByClusterIdAndType(clusterId, databaseType.name());
    }

    public Set<RDSConfig> findUserManagedByClusterId(Long clusterId) {
        return rdsConfigRepository.findUserManagedByClusterId(clusterId);
    }

    public void deleteDefaultRdsConfigs(Set<RDSConfig> rdsConfigs) {
        delete(rdsConfigs.stream()
                .filter(rdsConfig -> ResourceStatus.DEFAULT == rdsConfig.getStatus())
                .collect(Collectors.toSet())
        );
    }

    public Set<RDSConfig> findAllByWorkspaceId(Long workspaceId) {
        return rdsConfigRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public EnvironmentResourceRepository<RDSConfig, Long> repository() {
        return rdsConfigRepository;
    }

    @Override
    public Set<Cluster> getClustersUsingResource(RDSConfig rdsConfig) {
        return clusterService.findByRdsConfig(rdsConfig.getId());
    }

    @Override
    public Set<Cluster> getClustersUsingResourceInEnvironment(RDSConfig rdsConfig, Long environmentId) {
        return clusterService.findAllClustersByRdsConfigInEnvironment(rdsConfig, environmentId);
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.DATABASE;
    }

    public String testRdsConnection(Long workspaceId, String existingRDSConfigName, RDSConfig existingRds) {
        if (existingRDSConfigName != null) {
            return testRdsConnection(existingRDSConfigName, workspaceId);
        } else if (existingRds != null) {
            return testRdsConnection(existingRds);
        }
        throw new BadRequestException("Either an Database id, name or an Database request needs to be specified in the request. ");
    }

    private String testRdsConnection(String existingRDSConfigName, Long workspaceId) {
        User user = getLoggedInUser();
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        try {
            RDSConfig config = getByNameForWorkspace(existingRDSConfigName, workspace);
            return testRdsConnection(resolveVaultValues(config));
        } catch (AccessDeniedException | NotFoundException e) {
            return "access is denied";
        }
    }

    private String testRdsConnection(RDSConfig rdsConfig) {
        try {
            if (rdsConfig == null) {
                return "access is denied";
            }
            rdsConnectionValidator.validateRdsConnection(rdsConfig);
            return "connected";
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    public RDSConfig resolveVaultValues(RDSConfig config) {
        String username = config.getConnectionUserName();
        String password = config.getConnectionPassword();
        config.setConnectionUserName(username);
        config.setConnectionPassword(password);
        return config;
    }

}
