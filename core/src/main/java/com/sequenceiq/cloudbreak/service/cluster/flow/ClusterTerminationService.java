package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.util.JsonUtil.readValue;
import static com.sequenceiq.cloudbreak.util.JsonUtil.writeValueAsString;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.ConstraintRepository;
import com.sequenceiq.cloudbreak.repository.ContainerRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationFailedException;

@Component
public class ClusterTerminationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTerminationService.class);

    private static final String DELIMITER = "_";

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Resource
    private Map<FileSystemType, FileSystemConfigurator> fileSystemConfigurators;

    @Inject
    private ConstraintRepository constraintRepository;

    @Inject
    private ContainerRepository containerRepository;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private TransactionService transactionService;

    public Boolean deleteClusterComponents(Long clusterId) {
        Cluster cluster = clusterRepository.findById(clusterId);
        if (cluster == null) {
            LOGGER.warn("Failed to delete containers of cluster (id:'{}'), because the cluster could not be found in the database.", clusterId);
            return Boolean.TRUE;
        }
        OrchestratorType orchestratorType;
        try {
            orchestratorType = orchestratorTypeResolver.resolveType(cluster.getStack().getOrchestrator().getType());
        } catch (CloudbreakException e) {
            throw new TerminationFailedException(String.format("Failed to delete containers of cluster (id:'%s',name:'%s').",
                    cluster.getId(), cluster.getName()), e);
        }
        if (orchestratorType.hostOrchestrator()) {
            return Boolean.TRUE;
        }
        return deleteClusterContainers(cluster);
    }

    public Boolean deleteClusterContainers(Cluster cluster) {
        try {
            Orchestrator orchestrator = cluster.getStack().getOrchestrator();
            ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get(orchestrator.getType());
            try {
                Map<String, Object> map = new HashMap<>(orchestrator.getAttributes().getMap());
                OrchestrationCredential credential = new OrchestrationCredential(orchestrator.getApiEndpoint(), map);
                Set<Container> containers = containerRepository.findContainersInCluster(cluster.getId());
                List<ContainerInfo> containerInfo = containers.stream()
                        .map(c -> new ContainerInfo(c.getContainerId(), c.getName(), c.getHost(), c.getImage())).collect(Collectors.toList());
                containerOrchestrator.deleteContainer(containerInfo, credential);
                transactionService.required(() -> {
                    containerRepository.delete(containers);
                    deleteClusterHostGroupsWithItsMetadata(cluster);
                    return null;
                });
            } catch (TransactionExecutionException | CloudbreakOrchestratorException e) {
                throw new TerminationFailedException(String.format("Failed to delete containers of cluster (id:'%s',name:'%s').",
                        cluster.getId(), cluster.getName()), e);
            }
            return Boolean.TRUE;
        } catch (CloudbreakException ignored) {
            return Boolean.FALSE;
        }
    }

    public void finalizeClusterTermination(Long clusterId) throws TransactionExecutionException {
        Cluster cluster = clusterRepository.findById(clusterId);
        Set<RDSConfig> rdsConfigs = cluster.getRdsConfigs();
        Long stackId = cluster.getStack().getId();
        String terminatedName = cluster.getName() + DELIMITER + new Date().getTime();
        cluster.setName(terminatedName);
        FileSystem fs = cluster.getFileSystem();
        if (fs != null) {
            deleteFileSystemResources(stackId, fs);
        }
        cluster.setBlueprint(null);
        cluster.setStack(null);
        cluster.setLdapConfig(null);
        cluster.setRdsConfigs(new HashSet<>());
        cluster.setProxyConfig(null);
        cluster.setStatus(DELETE_COMPLETED);
        transactionService.required(() -> {
            deleteClusterHostGroupsWithItsMetadata(cluster);
            rdsConfigService.deleteDefaultRdsConfigs(rdsConfigs);
            componentConfigProvider.deleteComponentsForStack(stackId);
            return null;
        });
    }

    private void deleteClusterHostGroupsWithItsMetadata(Cluster cluster) {
        Set<HostGroup> hostGroups = hostGroupRepository.findHostGroupsInCluster(cluster.getId());
        Collection<Constraint> constraintsToDelete = new LinkedList<>();
        for (HostGroup hg : hostGroups) {
            hg.getRecipes().clear();
            Constraint constraint = hg.getConstraint();
            if (constraint != null) {
                constraintsToDelete.add(constraint);
            }
        }
        hostGroupRepository.delete(hostGroups);
        constraintRepository.delete(constraintsToDelete);
        cluster.getHostGroups().clear();
        cluster.getContainers().clear();
        clusterRepository.save(cluster);
    }

    private void deleteFileSystemResources(Long stackId, FileSystem fileSystem) {
        try {
            FileSystemConfigurator fsConfigurator = fileSystemConfigurators.get(FileSystemType.valueOf(fileSystem.getType()));
            String json = writeValueAsString(fileSystem.getProperties());
            FileSystemConfiguration fsConfiguration = readValue(json, FileSystemType.valueOf(fileSystem.getType()).getClazz());
            fsConfiguration.addProperty(FileSystemConfiguration.STORAGE_CONTAINER, "cloudbreak" + stackId);
            fsConfigurator.deleteResources(fsConfiguration);
        } catch (IOException e) {
            throw new TerminationFailedException("File system resources could not be deleted: ", e);
        }
    }
}
