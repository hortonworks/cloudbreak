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
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RdsConfigRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Service
public class RdsConfigService extends AbstractWorkspaceAwareResourceService<RDSConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsConfigService.class);

    @Inject
    private RdsConfigRepository rdsConfigRepository;

    @Inject
    private ClusterService clusterService;

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
        prepareDeletion(rdsConfig);
        if (!isRdsInUseByOthers(rdsConfig)) {
            LOGGER.debug("Archiving RDS config with name: {}", rdsConfig.getName());
            rdsConfig.setArchived(true);
            rdsConfig.setDeletionTimestamp(System.currentTimeMillis());
            rdsConfig.unsetRelationsToEntitiesToBeDeleted();
            repository().save(rdsConfig);
        }
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

    }

    private boolean isRdsInUseByOthers(RDSConfig resource) {
        boolean inUse = false;
        Set<Cluster> clustersWithThisRds = getClustersUsingResource(resource);
        if (!clustersWithThisRds.isEmpty()) {
            String clusters = clustersWithThisRds
                    .stream()
                    .map(Cluster::getName)
                    .collect(Collectors.joining(", "));
            LOGGER.info("{} will not be deleted because there are clusters associated with it: {}, this is quite normal, "
                    + "since we are reusing the sdx database.", resource.getName(), clusters);
            inUse = true;
        }
        return inUse;
    }

    @Override
    protected void prepareCreation(RDSConfig resource) {

    }

    public Set<Cluster> getClustersUsingResource(RDSConfig rdsConfig) {
        return clusterService.findByRdsConfig(rdsConfig.getId());
    }

    public RDSConfig resolveVaultValues(RDSConfig config) {
        String username = config.getConnectionUserName();
        String password = config.getConnectionPassword();
        config.setConnectionUserName(username);
        config.setConnectionPassword(password);
        return config;
    }

}
