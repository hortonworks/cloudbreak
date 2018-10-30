package com.sequenceiq.cloudbreak.service.rdsconfig;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
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
import com.sequenceiq.cloudbreak.service.VaultService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.AbstractEnvironmentAwareService;
import com.sequenceiq.cloudbreak.service.stack.StackApiViewService;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
public class RdsConfigService extends AbstractEnvironmentAwareService<RDSConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsConfigService.class);

    @Inject
    private RdsConfigRepository rdsConfigRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackApiViewService stackApiViewService;

    @Inject
    private RdsConnectionValidator rdsConnectionValidator;

    @Inject
    private TransactionService transactionService;

    @Inject
    private VaultService vaultService;

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
        RDSConfig configByName = rdsConfigRepository.findByNameAndWorkspaceId(rdsConfig.getName(), workspaceId);
        if (configByName == null) {
            Workspace workspace = getWorkspaceService().get(workspaceId, user);
            return create(rdsConfig, workspace, user);
        }
        return rdsConfig;

    }

    public Set<RDSConfig> findByClusterId(Long clusterId) {
        return rdsConfigRepository.findByClusterId(clusterId);
    }

    public RDSConfig findByClusterIdAndType(Long clusterId, RdsType rdsType) {
        return rdsConfigRepository.findByClusterIdAndType(clusterId, rdsType.name());
    }

    public Set<RDSConfig> findUserManagedByClusterId(Long clusterId) {
        return rdsConfigRepository.findUserManagedByClusterId(clusterId);
    }

    public void deleteDefaultRdsConfigs(Set<RDSConfig> rdsConfigs) {
        rdsConfigs.stream().filter(rdsConfig -> ResourceStatus.DEFAULT == rdsConfig.getStatus()).forEach(this::setStatusToDeleted);
    }

    private void setStatusToDeleted(RDSConfig rdsConfig) {
        rdsConfig.setName(NameUtil.postfixWithTimestamp(rdsConfig.getName()));
        rdsConfig.setStatus(ResourceStatus.DEFAULT_DELETED);
        rdsConfigRepository.save(rdsConfig);
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
        return WorkspaceResource.RDS;
    }

    @Override
    protected void prepareDeletion(RDSConfig rdsConfig) {
        checkClustersForDeletion(rdsConfig);
        if (!ResourceStatus.USER_MANAGED.equals(rdsConfig.getStatus())) {
            setStatusToDeleted(rdsConfig);
            throw new BadRequestException(String.format("RDS config '%s' is not user managed", rdsConfig.getName()));
        }
    }

    @Override
    protected void prepareCreation(RDSConfig resource) {
    }

    public String testRdsConnection(String existingRDSConfigName, Workspace workspace) {
        try {
            RDSConfig config = getByNameForWorkspace(existingRDSConfigName, workspace);
            return testRdsConnection(resolveVaultValues(config));
        } catch (AccessDeniedException | NotFoundException e) {
            return "access is denied";
        }
    }

    public RDSConfig resolveVaultValues(RDSConfig config) {
        String username = vaultService.resolveSingleValue(config.getConnectionUserName());
        String password = vaultService.resolveSingleValue(config.getConnectionPassword());
        config.setConnectionUserName(username);
        config.setConnectionPassword(password);
        return config;
    }

    public String testRdsConnection(RDSConfig rdsConfig) {
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
}
