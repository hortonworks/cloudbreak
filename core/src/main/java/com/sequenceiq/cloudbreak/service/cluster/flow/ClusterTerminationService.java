package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.util.JsonUtil.readValue;
import static com.sequenceiq.cloudbreak.util.JsonUtil.writeValueAsString;

import java.io.IOException;
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
import javax.transaction.Transactional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.ConstraintRepository;
import com.sequenceiq.cloudbreak.repository.ContainerRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationFailedException;

@Component
@Transactional
public class ClusterTerminationService {

    private static final String DELIMITER = "_";

    @Inject
    private ClusterRepository clusterRepository;
    @Inject
    private HostGroupRepository hostGroupRepository;
    @Resource
    private Map<FileSystemType, FileSystemConfigurator> fileSystemConfigurators;
    @Inject
    private HostMetadataRepository hostMetadataRepository;
    @Inject
    private ConstraintRepository constraintRepository;
    @Inject
    private ContainerRepository containerRepository;
    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;
    @Inject
    private TlsSecurityService tlsSecurityService;

    //TODO check orchestrators
    public void deleteClusterContainers(Long clusterId) {
        Cluster cluster = clusterRepository.findById(clusterId);
        try {
            if (cluster != null) {
                Orchestrator orchestrator = cluster.getStack().getOrchestrator();
                Map<String, Object> map = new HashMap<>();
                map.putAll(orchestrator.getAttributes().getMap());
                map.put("certificateDir", tlsSecurityService.prepareCertDir(cluster.getStack().getId()));
                OrchestrationCredential credential = new OrchestrationCredential(orchestrator.getApiEndpoint(), map);
                ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get(orchestrator.getType());
                Set<Container> containers = containerRepository.findContainersInCluster(cluster.getId());
                List<ContainerInfo> containerInfo = containers.stream()
                        .map(c -> new ContainerInfo(c.getContainerId(), c.getName(), c.getHost(), c.getImage())).collect(Collectors.toList());
                containerOrchestrator.deleteContainer(containerInfo, credential);
                containerRepository.delete(containers);
                deleteClusterHostGroupsWithItsMetadata(cluster);
            } else {
                String msg = String.format("Failed to delete containers of cluster (id:'%s'), because the cluster could not be found in the database.",
                        clusterId);
                throw new TerminationFailedException(msg);
            }
        } catch (CloudbreakException | CloudbreakOrchestratorException e) {
            throw new TerminationFailedException(String.format("Failed to delete containers of cluster (id:'%s',name:'%s').", clusterId, cluster.getName()), e);
        }
    }

    public void finalizeClusterTermination(Long clusterId) {
        Cluster cluster = clusterRepository.findById(clusterId);
        String terminatedName = cluster.getName() + DELIMITER + new Date().getTime();
        cluster.setName(terminatedName);
        FileSystem fs = cluster.getFileSystem();
        if (fs != null) {
            deleteFileSystemResources(cluster.getStack().getId(), fs);
        }
        cluster.setBlueprint(null);
        cluster.setStack(null);
        cluster.setSssdConfig(null);
        cluster.setStatus(DELETE_COMPLETED);
        deleteClusterHostGroupsWithItsMetadata(cluster);
    }

    private void deleteClusterHostGroupsWithItsMetadata(Cluster cluster) {
        Set<HostGroup> hostGroups = new HashSet<>(cluster.getHostGroups());
        List<Constraint> constraintsToDelete = new LinkedList<>();
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
        cluster.getHostServices().clear();
        clusterRepository.save(cluster);
    }

    private Map<String, String> deleteFileSystemResources(Long stackId, FileSystem fileSystem) {
        try {
            FileSystemConfigurator fsConfigurator = fileSystemConfigurators.get(FileSystemType.valueOf(fileSystem.getType()));
            String json = writeValueAsString(fileSystem.getProperties());
            FileSystemConfiguration fsConfiguration = (FileSystemConfiguration) readValue(json, FileSystemType.valueOf(fileSystem.getType()).getClazz());
            fsConfiguration.addProperty(FileSystemConfiguration.STORAGE_CONTAINER, "cloudbreak" + stackId);
            return fsConfigurator.deleteResources(fsConfiguration);
        } catch (IOException e) {
            throw new TerminationFailedException("File system resources could not be deleted: ", e);
        }
    }
}
