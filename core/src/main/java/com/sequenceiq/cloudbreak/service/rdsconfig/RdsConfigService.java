package com.sequenceiq.cloudbreak.service.rdsconfig;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentResourceRepository;
import com.sequenceiq.cloudbreak.repository.RdsConfigRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.AbstractEnvironmentAwareService;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
public class RdsConfigService extends AbstractEnvironmentAwareService<RDSConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsConfigService.class);

    @Inject
    private RdsConfigRepository rdsConfigRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private RdsConnectionValidator rdsConnectionValidator;

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

    private void checkRdsConfigNotAssociated(RDSConfig rdsConfig) {
        LOGGER.info("Deleting rds configuration with name: {}", rdsConfig.getName());
        List<Cluster> clustersWithProvidedRds = new ArrayList<>(clusterService.findAllClustersByRDSConfig(rdsConfig.getId()));
        if (!clustersWithProvidedRds.isEmpty()) {
            if (clustersWithProvidedRds.size() > 1) {
                String clusters = clustersWithProvidedRds
                        .stream()
                        .map(Cluster::getName)
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format(
                        "There are clusters associated with RDS config '%s'. Please remove these before deleting the RDS configuration. "
                                + "The following clusters are using this RDS: [%s]", rdsConfig.getName(), clusters));
            } else {
                throw new BadRequestException(String.format("There is a cluster ['%s'] which uses RDS config '%s'. Please remove this "
                        + "cluster before deleting the RDS", clustersWithProvidedRds.get(0).getName(), rdsConfig.getName()));
            }
        }
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
    public WorkspaceResource resource() {
        return WorkspaceResource.RDS;
    }

    @Override
    protected void prepareDeletion(RDSConfig resource) {
        checkRdsConfigNotAssociated(resource);
        if (!ResourceStatus.USER_MANAGED.equals(resource.getStatus())) {
            setStatusToDeleted(resource);
            throw new BadRequestException(String.format("RDS config '%s' is not usr managed", resource.getName()));
        }
    }

    @Override
    protected void prepareCreation(RDSConfig resource) {
    }

    public String testRdsConnection(String existingRDSConfigName, Workspace workspace) {
        try {
            RDSConfig config = getByNameForWorkspace(existingRDSConfigName, workspace);
            return testRdsConnection(config);
        } catch (AccessDeniedException | NotFoundException e) {
            return "access is denied";
        }
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
