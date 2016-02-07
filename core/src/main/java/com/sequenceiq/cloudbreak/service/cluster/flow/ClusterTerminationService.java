package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.util.JsonUtil.readValue;
import static com.sequenceiq.cloudbreak.util.JsonUtil.writeValueAsString;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.transaction.Transactional;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.ConstraintRepository;
import com.sequenceiq.cloudbreak.repository.ContainerRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationFailedException;
import org.springframework.stereotype.Component;

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
    private ContainerOrchestratorResolver orchestratorResolver;

    public void deleteClusterContainers(Long clusterId) {
        Cluster cluster = clusterRepository.findById(clusterId);
        try {
            if (cluster != null) {
                Orchestrator orchestrator = cluster.getStack().getOrchestrator();
                OrchestrationCredential credential = new OrchestrationCredential(orchestrator.getApiEndpoint(), orchestrator.getAttributes().getMap());
                ContainerOrchestrator containerOrchestrator = orchestratorResolver.get(orchestrator.getType());
                Set<Container> containers = containerRepository.findContainersInCluster(cluster.getId());
                List<ContainerInfo> containerInfo = FluentIterable.from(containers).transform(new Function<Container, ContainerInfo>() {
                    @Nullable
                    @Override
                    public ContainerInfo apply(Container input) {
                        return new ContainerInfo(input.getContainerId(), input.getName(), input.getHost(), input.getImage());
                    }
                }).toList();
                containerOrchestrator.deleteContainer(containerInfo, credential);
                containerRepository.delete(containers);
            } else {
                String msg = String.format("Failed to delete containers of cluster (id:'%s',name:'%s'), the cluster is not associated to stack.",
                        clusterId, cluster.getName());
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
        cluster.setBlueprint(null);
        cluster.setStack(null);
        clusterRepository.save(cluster);
        for (HostGroup hostGroup : hostGroupRepository.findHostGroupsInCluster(cluster.getId())) {
            hostGroup.getRecipes().clear();
            hostGroup.getHostMetadata().clear();
            Long constraintId = hostGroup.getConstraint().getId();
            hostGroup.setConstraint(null);
            hostGroupRepository.save(hostGroup);
            constraintRepository.delete(constraintId);
        }
        FileSystem fs = cluster.getFileSystem();
        if (fs != null) {
            deleteFileSystemResources(cluster.getStack().getId(), fs);
        }
        for (HostMetadata metadata : hostMetadataRepository.findHostsInCluster(cluster.getId())) {
            hostMetadataRepository.delete(metadata.getId());
        }

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
