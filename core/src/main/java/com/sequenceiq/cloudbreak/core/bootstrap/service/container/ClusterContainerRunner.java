package com.sequenceiq.cloudbreak.core.bootstrap.service.container;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_AGENT;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_DB;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_SERVER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ContainerConfigService;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ContainerService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterContainerRunner {

    private static final String NONE = "none";

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackService stackService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private ContainerConfigService containerConfigService;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Inject
    private ContainerService containerService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private ContainerConstraintFactory constraintFactory;

    public Map<String, List<Container>> runClusterContainers(Stack stack) throws CloudbreakException {
        try {
            String cloudPlatform = StringUtils.isNotEmpty(stack.cloudPlatform()) ? stack.cloudPlatform() : NONE;
            return initializeClusterContainers(stack, cloudPlatform);
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    public Map<String, List<Container>> addClusterContainers(Long stackId, String hostGroupName, Integer scalingAdjustment)
            throws CloudbreakException {
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            String cloudPlatform = StringUtils.isNotEmpty(stack.cloudPlatform()) ? stack.cloudPlatform() : NONE;
            return addClusterContainers(stack, cloudPlatform, hostGroupName, scalingAdjustment);
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    private Map<String, List<Container>> initializeClusterContainers(Stack stack, String cloudPlatform)
            throws CloudbreakException, CloudbreakOrchestratorException {

        Orchestrator orchestrator = stack.getOrchestrator();
        Map<String, Object> map = new HashMap<>(orchestrator.getAttributes().getMap());
        OrchestrationCredential credential = new OrchestrationCredential(orchestrator.getApiEndpoint(), map);
        ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get(orchestrator.getType());
        Map<String, List<ContainerInfo>> containers = new HashMap<>();
        Cluster cluster = clusterService.retrieveClusterByStackIdWithoutAuth(stack.getId())
                .orElseThrow(NotFoundException.notFound("clsuter", stack.getId()));

        String gatewayHostname = getGatewayHostName(stack);

        try {
            ContainerConstraint ambariServerDbConstraint = constraintFactory.getAmbariServerDbConstraint(gatewayHostname,
                    cluster.getName(), cluster.getId().toString());
            List<ContainerInfo> dbContainer = containerOrchestrator.runContainer(containerConfigService.get(stack, AMBARI_DB), credential,
                    ambariServerDbConstraint, clusterDeletionBasedModel(stack.getId(), cluster.getId()));
            containers.put(AMBARI_DB.name(), dbContainer);

            String serverDbHostName = dbContainer.get(0).getHost();
            ContainerConstraint ambariServerConstraint = constraintFactory.getAmbariServerConstraint(serverDbHostName, gatewayHostname,
                    cloudPlatform, cluster.getName(), cluster.getId().toString());
            List<ContainerInfo> ambariServerContainer = containerOrchestrator.runContainer(containerConfigService.get(stack, AMBARI_SERVER),
                    credential, ambariServerConstraint, clusterDeletionBasedModel(stack.getId(), cluster.getId()));
            containers.put(AMBARI_SERVER.name(), ambariServerContainer);
            String ambariServerHost = ambariServerContainer.get(0).getHost();

            List<String> hostBlackList = new ArrayList<>();
            for (HostGroup hostGroup : hostGroupService.findHostGroupsInCluster(stack.getCluster().getId())) {
                ContainerConstraint ambariAgentConstraint = constraintFactory.getAmbariAgentConstraint(ambariServerHost, null, cloudPlatform, hostGroup,
                        null, hostBlackList, cluster.getId().toString());
                List<ContainerInfo> containerInfos = containerOrchestrator.runContainer(containerConfigService.get(stack, AMBARI_AGENT), credential,
                        ambariAgentConstraint, clusterDeletionBasedModel(stack.getId(), cluster.getId()));
                containers.put(hostGroup.getName(), containerInfos);
                hostBlackList.addAll(getHostsFromContainerInfo(containerInfos));
            }

            return saveContainers(containers, cluster);
        } catch (CloudbreakOrchestratorException ex) {
            if (!containers.isEmpty()) {
                saveContainers(containers, cluster);
            }
            checkCancellation(ex);
            throw ex;
        }
    }

    private String getGatewayHostName(Stack stack) {
        String gatewayHostname = "";
        if (stack.getInstanceGroups() != null && !stack.getInstanceGroups().isEmpty()) {
            if (stack.getPrimaryGatewayInstance() != null) {
                InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
                gatewayHostname = gatewayInstance.getDiscoveryFQDN();
            }
        }
        return gatewayHostname;
    }

    private String getGatewayPrivateIp(Stack stack) {
        String gatewayHostname = "";
        if (stack.getInstanceGroups() != null && !stack.getInstanceGroups().isEmpty()) {
            InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
            gatewayHostname = gatewayInstance.getPrivateIp();
        }
        return gatewayHostname;
    }

    private Map<String, List<Container>> addClusterContainers(Stack stack, String cloudPlatform, String hostGroupName, Integer adjustment)
            throws CloudbreakException, CloudbreakOrchestratorException {

        Orchestrator orchestrator = stack.getOrchestrator();
        Map<String, Object> map = new HashMap<>(orchestrator.getAttributes().getMap());
        OrchestrationCredential credential = new OrchestrationCredential(orchestrator.getApiEndpoint(), map);
        ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get(orchestrator.getType());

        Cluster cluster = clusterService.retrieveClusterByStackIdWithoutAuth(stack.getId())
                .orElseThrow(NotFoundException.notFound("cluster", stack.getId()));

        try {
            Set<Container> existingContainers = containerService.findContainersInCluster(cluster.getId());
            String ambariServerHost = existingContainers.stream()
                    .filter(input -> input.getImage().contains(AMBARI_SERVER.getName()))
                    .findFirst().get().getHost();
            HostGroup hostGroup = hostGroupService.findHostGroupInClusterByName(cluster.getId(), hostGroupName)
                    .orElseThrow(NotFoundException.notFound("hostgroup", hostGroupName));
            String ambariAgentApp = existingContainers.stream()
                    .filter(input -> hostGroup.getHostNames().contains(input.getHost()) && input.getImage().contains(AMBARI_AGENT.getName()))
                    .findFirst().get().getName();
            List<String> hostBlackList = getOtherHostgroupsAgentHostsFromContainer(existingContainers, hostGroupName);
            ContainerConstraint ambariAgentConstraint = constraintFactory.getAmbariAgentConstraint(ambariServerHost, ambariAgentApp,
                    cloudPlatform, hostGroup, adjustment, hostBlackList, cluster.getId().toString());
            Map<String, List<ContainerInfo>> containers = new HashMap<>();
            containers.put(hostGroup.getName(), containerOrchestrator.runContainer(containerConfigService.get(stack, AMBARI_AGENT), credential,
                    ambariAgentConstraint, clusterDeletionBasedModel(stack.getId(), cluster.getId())));

            return saveContainers(containers, cluster);
        } catch (CloudbreakOrchestratorException | NotFoundException ex) {
            checkCancellation(ex);
            throw ex;
        }
    }

    private List<String> getOtherHostgroupsAgentHostsFromContainer(Collection<Container> existingContainers, String hostGroupName) {
        String hostGroupNamePart = hostGroupName.replace("_", "-");
        return existingContainers.stream()
                .filter(input -> input.getImage().contains(AMBARI_AGENT.getName()) && !input.getName().contains(hostGroupNamePart))
                .map(Container::getHost).collect(Collectors.toList());
    }

    private Collection<String> getHostsFromContainerInfo(Collection<ContainerInfo> containerInfos) {
        return containerInfos.stream().map(ContainerInfo::getHost).collect(Collectors.toList());
    }

    private List<String> getHosts(Stack stack) {
        List<String> hosts = new ArrayList<>();
        for (InstanceMetaData instanceMetaData : stack.getNotDeletedInstanceMetaDataSet()) {
            hosts.add(instanceMetaData.getDiscoveryFQDN());
        }
        return hosts;
    }

    private List<Container> convert(Iterable<ContainerInfo> containerInfo, Cluster cluster) {
        List<Container> containers = new ArrayList<>();
        for (ContainerInfo source : containerInfo) {
            Container container = converterUtil.convert(source, Container.class);
            container.setCluster(cluster);
            containers.add(container);
        }
        return containers;
    }

    private Map<String, List<Container>> saveContainers(Map<String, List<ContainerInfo>> containerInfo, Cluster cluster) {
        Map<String, List<Container>> containers = new HashMap<>();
        for (Entry<String, List<ContainerInfo>> containerInfoEntry : containerInfo.entrySet()) {
            List<Container> hostGroupContainers = convert(containerInfoEntry.getValue(), cluster);
            containers.put(containerInfoEntry.getKey(), hostGroupContainers);
            containerService.save(hostGroupContainers);
        }
        return containers;
    }

    private void checkCancellation(Exception ex) {
        if (ex instanceof CloudbreakOrchestratorCancelledException || ExceptionUtils.getRootCause(ex) instanceof CloudbreakOrchestratorCancelledException) {
            throw new CancellationException("Creation of cluster containers was cancelled.");
        }
    }
}

