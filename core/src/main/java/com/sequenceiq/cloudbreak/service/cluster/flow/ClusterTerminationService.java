package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.services.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationsViewProvider;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.repository.ConstraintRepository;
import com.sequenceiq.cloudbreak.repository.ContainerRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationFailedException;

@Component
public class ClusterTerminationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTerminationService.class);

    private static final String DELIMITER = "_";

    @Inject
    private ClusterService clusterService;

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

    @Inject
    private FileSystemConfigurationsViewProvider fileSystemConfigurationsViewProvider;

    public Boolean deleteClusterComponents(Long clusterId) {
        Optional<Cluster> clusterOpt = clusterService.findById(clusterId);
        if (!clusterOpt.isPresent()) {
            LOGGER.debug("Failed to delete containers of cluster (id:'{}'), because the cluster could not be found in the database.", clusterId);
            return Boolean.TRUE;
        }
        Cluster cluster = clusterOpt.get();
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
                    containerRepository.deleteAll(containers);
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
        Cluster cluster = clusterService.findOneWithLists(clusterId);
        Set<RDSConfig> rdsConfigs = cluster.getRdsConfigs();
        Long stackId = cluster.getStack().getId();
        String terminatedName = cluster.getName() + DELIMITER + new Date().getTime();
        cluster.setName(terminatedName);
        FileSystem fs = cluster.getFileSystem();
        if (fs != null) {
            deleteFileSystemResources(stackId, fs);
        }
        cluster.setClusterDefinition(null);
        cluster.setStack(null);
        cluster.setLdapConfig(null);
        cluster.setRdsConfigs(new HashSet<>());
        cluster.setProxyConfig(null);
        cluster.setStatus(DELETE_COMPLETED);
        cluster.setFileSystem(null);
        cluster.setEnvironment(null);
        cluster.setKerberosConfig(null);
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
        hostGroupRepository.deleteAll(hostGroups);
        constraintRepository.deleteAll(constraintsToDelete);
        cluster.getHostGroups().clear();
        cluster.getContainers().clear();
        clusterService.save(cluster);
    }

    private void deleteFileSystemResources(Long stackId, FileSystem fileSystem) {
        try {
            FileSystemConfigurator fsConfigurator = fileSystemConfigurators.get(fileSystem.getType());
            BaseFileSystemConfigurationsView fsConfiguration = fileSystemConfigurationsViewProvider.propagateConfigurationsView(fileSystem);
            fsConfiguration.setStorageContainer("cloudbreak" + stackId);
            fsConfigurator.deleteResources(fsConfiguration);
        } catch (IOException e) {
            throw new TerminationFailedException("File system resources could not be deleted: ", e);
        }
    }
}
