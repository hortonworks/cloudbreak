package com.sequenceiq.cloudbreak.service.rdsconfig;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RdsConfigRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;

@Service
public class RdsConfigService extends AbstractWorkspaceAwareResourceService<RDSConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsConfigService.class);

    @Inject
    private RdsConfigRepository rdsConfigRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private RdsConnectionValidator rdsConnectionValidator;

    @Inject
    private TransactionService transactionService;

    public Set<RDSConfig> findByNamesInWorkspace(Set<String> names, @NotNull Long workspaceId) {
        return CollectionUtils.isEmpty(names) ? new HashSet<>() : rdsConfigRepository.findAllByNameInAndWorkspaceId(names, workspaceId);
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

    @Override
    public RDSConfig delete(RDSConfig rdsConfig) {
        MDCBuilder.buildMdcContext(rdsConfig);
        LOGGER.debug("Archiving {} with name: {}", resource().getReadableName(), rdsConfig.getName());
        prepareDeletion(rdsConfig);
        rdsConfig.setArchived(true);
        rdsConfig.setDeletionTimestamp(System.currentTimeMillis());
        rdsConfig.unsetRelationsToEntitiesToBeDeleted();
        repository().save(rdsConfig);
        return rdsConfig;
    }

    public RDSConfig createIfNotExists(User user, RDSConfig rdsConfig, Long workspaceId) {
        Optional<RDSConfig> configByName = rdsConfigRepository.findByNameAndWorkspaceId(rdsConfig.getName(), workspaceId);
        if (configByName.isEmpty()) {
            Workspace workspace = getWorkspaceService().get(workspaceId, user);
            return createWithMdcContextRestore(rdsConfig, workspace, user);
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
        Map<String, String> mdcContextMap = MDCBuilder.getMdcContextMap();
        rdsConfigs.stream()
                .filter(rdsConfig -> ResourceStatus.DEFAULT == rdsConfig.getStatus())
                .forEach(this::delete);
        MDCBuilder.buildMdcContextFromMap(mdcContextMap);
    }

    public Set<RDSConfig> findAllByWorkspaceId(Long workspaceId) {
        return rdsConfigRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public WorkspaceResourceRepository<RDSConfig, Long> repository() {
        return rdsConfigRepository;
    }

    @Override
    protected void prepareDeletion(RDSConfig resource) {
        Set<Cluster> clustersWithThisProxy = getClustersUsingResource(resource);
        if (!clustersWithThisProxy.isEmpty()) {
            String clusters = clustersWithThisProxy
                    .stream()
                    .map(Cluster::getName)
                    .collect(Collectors.joining(", "));
            throw new BadRequestException(String.format(resource().getReadableName() + " '%s' cannot be deleted"
                    + " because there are clusters associated with it: [%s].", resource.getName(), clusters));
        }
    }

    @Override
    protected void prepareCreation(RDSConfig resource) {

    }

    public Set<Cluster> getClustersUsingResource(RDSConfig rdsConfig) {
        return clusterService.findByRdsConfig(rdsConfig.getId());
    }

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
