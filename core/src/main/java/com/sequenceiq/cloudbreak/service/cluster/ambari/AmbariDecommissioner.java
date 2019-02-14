package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.common.type.HostMetadataState.HEALTHY;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_AGENT;
import static com.sequenceiq.cloudbreak.service.PollingResult.SUCCESS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.DECOMMISSION_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.DECOMMISSION_SERVICES_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.START_SERVICES_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.STOP_SERVICES_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.DataNodeUtils.sortByUsedSpace;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.AMBARI_POLLING_INTERVAL;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.MAX_ATTEMPTS_FOR_HOSTS;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.services.ServiceAndHostService;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.repository.ContainerRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.NotEnoughNodeException;
import com.sequenceiq.cloudbreak.service.cluster.NotRecommendedNodeRemovalException;
import com.sequenceiq.cloudbreak.service.cluster.filter.ConfigParam;
import com.sequenceiq.cloudbreak.service.cluster.filter.HostFilterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClientPollerObject;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariDFSSpaceRetrievalTask;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariHostsLeaveStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.DNDecommissionStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.flow.RSDecommissionStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;
import com.sequenceiq.cloudbreak.util.AmbariClientExceptionUtil;
import com.sequenceiq.cloudbreak.util.StackUtil;

import groovyx.net.http.HttpResponseException;

@Component
public class AmbariDecommissioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariDecommissioner.class);

    private static final int MAX_ATTEMPTS_FOR_REGION_DECOM = 500;

    private static final String DATANODE = "DATANODE";

    private static final double SAFETY_PERCENTAGE = 1.2;

    private static final int NO_REPLICATION = 0;

    private static final Map<String, String> COMPONENTS_NEED_TO_DECOMMISSION = new HashMap<>();

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private PollingService<AmbariHostsWithNames> rsPollerService;

    @Inject
    private PollingService<AmbariClientPollerObject> ambariClientPollingService;

    @Inject
    private DNDecommissionStatusCheckerTask dnDecommissionStatusCheckerTask;

    @Inject
    private RSDecommissionStatusCheckerTask rsDecommissionStatusCheckerTask;

    @Inject
    private AmbariHostsLeaveStatusCheckerTask hostsLeaveStatusCheckerTask;

    @Inject
    private PollingService<AmbariHostsWithNames> ambariHostLeave;

    @Inject
    private AmbariOperationService ambariOperationService;

    @Inject
    private AmbariConfigurationService configurationService;

    @Inject
    private HostFilterService hostFilterService;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Inject
    private ContainerRepository containerRepository;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private AmbariDecommissionTimeCalculator ambariDecommissionTimeCalculator;

    @Inject
    private StackUtil stackUtil;

    @PostConstruct
    public void init() {
        COMPONENTS_NEED_TO_DECOMMISSION.put(DATANODE, "HDFS");
        COMPONENTS_NEED_TO_DECOMMISSION.put("NODEMANAGER", "YARN");
    }

    public Set<String> collectDownscaleCandidates(Stack stack, String hostGroupName, Integer scalingAdjustment) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        int adjustment = Math.abs(scalingAdjustment);
        Set<String> hostsToRemove = selectHostsToRemove(collectDownscaleCandidates(stack, cluster, hostGroupName, adjustment), adjustment);
        if (hostsToRemove.size() != adjustment) {
            throw new CloudbreakException(String.format("Only %d hosts found to downscale but %d required.", hostsToRemove.size(), adjustment));
        }
        return hostsToRemove;
    }

    public Set<String> decommissionAmbariNodes(Stack stack, Map<String, HostMetadata> hostsToRemove) throws CloudbreakException {
        AmbariClient ambariClient = getAmbariClient(stack);

        Map<String, HostMetadata> unhealthyHosts = new HashMap<>();
        Map<String, HostMetadata> healthyHosts = new HashMap<>();
        for (Entry<String, HostMetadata> hostToRemove : hostsToRemove.entrySet()) {
            if ("UNKNOWN".equals(ambariClient.getHostState(hostToRemove.getKey()))) {
                unhealthyHosts.put(hostToRemove.getKey(), hostToRemove.getValue());
            } else {
                healthyHosts.put(hostToRemove.getKey(), hostToRemove.getValue());
            }
        }

        Set<String> deletedHosts = new HashSet<>();
        Map<String, Map<String, String>> runningComponents = ambariClient.getHostComponentsStates();
        if (!unhealthyHosts.isEmpty()) {
            for (Entry<String, HostMetadata> host : unhealthyHosts.entrySet()) {
                deleteHostFromAmbari(host.getValue(), runningComponents, ambariClient);
                hostMetadataRepository.delete(host.getValue());
                deletedHosts.add(host.getKey());
            }
        }
        if (!healthyHosts.isEmpty()) {
            deletedHosts.addAll(decommissionAmbariNodes(stack, healthyHosts, runningComponents, ambariClient));
        }

        return deletedHosts;
    }

    private AmbariClient getAmbariClient(Stack stack) {
        Cluster cluster = stack.getCluster();
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp());
        return ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), cluster);
    }

    public Map<String, HostMetadata> collectHostsToRemove(Stack stack, String hostGroupName, Set<String> hostNames) {
        Map<String, HostMetadata> hostsToRemove = collectHostMetadata(stack.getCluster(), hostGroupName, hostNames);
        if (hostsToRemove.size() != hostNames.size()) {
            LOGGER.info("Not all the hosts found in the given host group.");
        }
        AmbariClient ambariClient = getAmbariClient(stack);
        List<String> runningHosts = ambariClient.getClusterHosts();
        if (runningHosts == null) {
            throw new OperationException("Running hosts is null");
        }
        Sets.newHashSet(hostsToRemove.keySet()).forEach(hostName -> {
            if (!runningHosts.contains(hostName)) {
                hostsToRemove.remove(hostName);
            }
        });
        return hostsToRemove;
    }

    public boolean deleteHostFromAmbari(Stack stack, HostMetadata data) {
        AmbariClient ambariClient = getAmbariClient(stack);
        Map<String, Map<String, String>> runningComponents = ambariClient.getHostComponentsStates();
        return deleteHostFromAmbari(data, runningComponents, ambariClient);
    }

    public Map<String, Long> selectNodes(Map<String, Long> sortedAscending, Collection<HostMetadata> filteredHostList, int removeCount) {
        LOGGER.debug("SortedAscending: {}, filteredHostList: {}", sortedAscending, filteredHostList);

        Map<String, Long> select = filteredHostList
                .stream()
                .filter(hostMetadata -> hostMetadata.getHostMetadataState() == HostMetadataState.UNHEALTHY
                        && sortedAscending.containsKey(hostMetadata.getHostName()))
                .limit(removeCount)
                .collect(Collectors.toMap(HostMetadata::getHostName, o -> 0L));

        if (select.size() < removeCount) {
            Set<String> hostNames = filteredHostList.stream().map(a -> a.getHostName().toLowerCase()).collect(Collectors.toSet());
            sortedAscending.entrySet().stream()
                    .filter(entry -> !select.keySet().contains(entry.getKey()) && hostNames.contains(entry.getKey().toLowerCase()))
                    .limit(removeCount - select.size())
                    .forEach(entry -> select.put(entry.getKey(), entry.getValue()));
        }
        return select;
    }

    private boolean deleteHostFromAmbari(HostMetadata data, Map<String, Map<String, String>> runningComponents, ServiceAndHostService ambariClient) {
        boolean hostDeleted = false;
        if (ambariClient.getClusterHosts().contains(data.getHostName())) {
            String hostState = ambariClient.getHostState(data.getHostName());
            if ("UNKNOWN".equals(hostState)) {
                deleteHosts(singletonList(data.getHostName()), runningComponents, ambariClient);
                hostDeleted = true;
            }
        } else {
            LOGGER.debug("Host is already deleted.");
            hostDeleted = true;
        }
        return hostDeleted;
    }

    private Collection<String> decommissionAmbariNodes(Stack stack, Map<String, HostMetadata> hostsToRemove, Map<String, Map<String, String>> runningComponents,
            AmbariClient ambariClient) throws CloudbreakException {
        Collection<String> result = new HashSet<>();
        PollingResult pollingResult = startServicesIfNeeded(stack, ambariClient, runningComponents);
        if (isSuccess(pollingResult)) {
            List<String> hostList = new ArrayList<>(hostsToRemove.keySet());
            Map<String, Integer> decommissionRequests = decommissionComponents(ambariClient, hostList, runningComponents, stack);
            if (!decommissionRequests.isEmpty()) {
                pollingResult =
                        ambariOperationService.waitForOperations(stack, ambariClient, decommissionRequests, DECOMMISSION_AMBARI_PROGRESS_STATE).getLeft();
            }
            if (isSuccess(pollingResult)) {
                pollingResult = waitForDataNodeDecommission(stack, ambariClient, hostList, runningComponents);
                if (isSuccess(pollingResult)) {
                    pollingResult = waitForRegionServerDecommission(stack, ambariClient, hostList, runningComponents);
                    if (isSuccess(pollingResult)) {
                        pollingResult = stopHadoopComponents(stack, ambariClient, hostList, runningComponents);
                        if (isSuccess(pollingResult)) {
                            deleteHosts(hostList, runningComponents, ambariClient);
                            result.addAll(hostsToRemove.keySet());
                        }
                    }
                }
            }
        }
        return result;
    }

    private void deleteHosts(Iterable<String> hosts, Map<String, Map<String, String>> components, ServiceAndHostService client) {
        for (String hostName : hosts) {
            client.deleteHostComponents(hostName, new ArrayList<>(components.get(hostName).keySet()));
            client.deleteHost(hostName);
        }
    }

    private List<HostMetadata> collectDownscaleCandidates(Stack stack, Cluster cluster, String hostGroupName, Integer scalingAdjustment) {
        List<HostMetadata> downScaleCandidates;
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp());
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(cluster.getId(), hostGroupName);
        Set<HostMetadata> hostsInHostGroup = hostGroup.getHostMetadata();
        List<HostMetadata> filteredHostList = hostFilterService.filterHostsForDecommission(cluster, hostsInHostGroup, hostGroupName);
        int reservedInstances = hostsInHostGroup.size() - filteredHostList.size();
        String blueprintName = cluster.getClusterDefinition().getStackName();
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), cluster);
        Map<String, List<String>> blueprintMap = ambariClient.getBlueprintMap(blueprintName);
        if (hostGroupNodesAreDataNodes(blueprintMap, hostGroupName)) {
            int replication = getReplicationFactor(ambariClient, hostGroupName);
            verifyNodeCount(replication, scalingAdjustment, filteredHostList.size(), reservedInstances, stack);
            downScaleCandidates = checkAndSortByAvailableSpace(stack, ambariClient, replication, scalingAdjustment, filteredHostList);
        } else {
            verifyNodeCount(NO_REPLICATION, scalingAdjustment, filteredHostList.size(), reservedInstances, stack);
            downScaleCandidates = filteredHostList;
        }
        return downScaleCandidates;
    }

    private boolean hostGroupNodesAreDataNodes(Map<String, List<String>> blueprintMap, String hostGroupName) {
        return blueprintMap.get(hostGroupName).contains(DATANODE);
    }

    public void verifyNodesAreRemovable(Stack stack, List<InstanceMetaData> instanceMetaDataList) {
        AmbariClient ambariClient = getAmbariClient(stack);
        Cluster cluster = stack.getCluster();
        String blueprintName = cluster.getClusterDefinition().getStackName();
        Map<String, List<String>> blueprintMap = ambariClient.getBlueprintMap(blueprintName);

        List<InstanceMetaData> instancesWithHostName = instanceMetaDataList.stream()
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                .collect(Collectors.toList());

        Multimap<Long, HostMetadata> hostGroupWithInstances = ArrayListMultimap.create();
        for (InstanceMetaData instanceMetaData : instancesWithHostName) {
            HostMetadata hostMetadata = hostGroupService.getHostMetadataByClusterAndHostName(stack.getCluster(), instanceMetaData.getDiscoveryFQDN());
            if (hostMetadata != null) {
                hostGroupWithInstances.put(hostMetadata.getHostGroup().getId(), hostMetadata);
            }
        }

        Set<HostGroup> hostGroups = hostGroupService.getByCluster(cluster.getId());
        for (HostGroup hostGroup : hostGroups) {
            Collection<HostMetadata> removableHostsInHostGroup = hostGroupWithInstances.get(hostGroup.getId());
            if (removableHostsInHostGroup != null && !removableHostsInHostGroup.isEmpty()) {
                String hostGroupName = hostGroup.getName();
                List<HostMetadata> hostListForDecommission = hostFilterService.filterHostsForDecommission(cluster, removableHostsInHostGroup, hostGroupName);
                boolean hostGroupContainsDatanode = hostGroupNodesAreDataNodes(blueprintMap, hostGroupName);
                int replication = hostGroupContainsDatanode ? getReplicationFactor(ambariClient, hostGroupName) : NO_REPLICATION;
                if (hostListForDecommission.size() < removableHostsInHostGroup.size()) {
                    List<HostMetadata> notRecommendedRemovableNodes = removableHostsInHostGroup.stream()
                            .filter(hostMetadata -> !hostListForDecommission.contains(hostMetadata))
                            .collect(Collectors.toList());
                    throw new NotRecommendedNodeRemovalException("Following nodes shouldn't be removed from the cluster: "
                            + notRecommendedRemovableNodes.stream().map(HostMetadata::getHostName).collect(Collectors.toList()));
                }
                verifyNodeCount(replication, removableHostsInHostGroup.size(), hostGroup.getHostMetadata().size(), 0, stack);
                if (hostGroupContainsDatanode) {
                    calculateDecommissioningTime(stack, hostListForDecommission, ambariClient);
                }
            }
        }
    }

    private Map<String, HostMetadata> collectHostMetadata(Cluster cluster, String hostGroupName, Collection<String> hostNames) {
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(cluster.getId(), hostGroupName);
        Set<HostMetadata> hostsInHostGroup = hostGroup.getHostMetadata();
        Map<String, HostMetadata> hostMetadatas = hostsInHostGroup.stream().filter(hostMetadata -> hostNames.contains(hostMetadata.getHostName())).collect(
                Collectors.toMap(HostMetadata::getHostName, hostMetadata -> hostMetadata));
        return hostMetadatas;
    }

    private int getReplicationFactor(ServiceAndHostService ambariClient, String hostGroup) {
        Map<String, String> configuration = configurationService.getConfiguration(ambariClient, hostGroup);
        return Integer.parseInt(configuration.get(ConfigParam.DFS_REPLICATION.key()));
    }

    private void verifyNodeCount(int replication, int scalingAdjustment, int hostSize, int reservedInstances, Stack stack) {
        boolean repairInProgress = stack.getDiskResources().stream()
                .map(resource -> stackUtil.getTypedAttributes(resource, VolumeSetAttributes.class))
                .flatMap(Optional::stream)
                .map(VolumeSetAttributes::getDeleteOnTermination)
                .anyMatch(Boolean.FALSE::equals);
        int adjustment = Math.abs(scalingAdjustment);
        if (!repairInProgress && (hostSize + reservedInstances - adjustment < replication || hostSize < adjustment)) {
            LOGGER.info("Cannot downscale: replication: {}, adjustment: {}, filtered host size: {}", replication, scalingAdjustment, hostSize);
            throw new NotEnoughNodeException("There is not enough node to downscale. "
                    + "Check the replication factor and the ApplicationMaster occupation.");
        }
    }

    private List<HostMetadata> checkAndSortByAvailableSpace(Stack stack, AmbariClient client, int replication, int adjustment,
            List<HostMetadata> filteredHostList) {
        int removeCount = Math.abs(adjustment);
        LOGGER.debug("RemoveCount: {}, replication: {}, filteredHostList size: {}, filteredHostList: {}",
                removeCount, replication, filteredHostList.size(), filteredHostList);
        Map<String, Map<Long, Long>> dfsSpace = getDFSSpace(stack, client);
        Map<String, Long> sortedAscending = sortByUsedSpace(dfsSpace, false);
        LOGGER.debug("SortedAscending: {}", sortedAscending);
        Map<String, Long> selectedNodes = selectNodes(sortedAscending, filteredHostList, removeCount);
        Map<String, Long> remainingNodes = removeSelected(sortedAscending, selectedNodes);
        LOGGER.debug("Selected nodes for decommission: {}", selectedNodes);
        LOGGER.debug("Remaining nodes after decommission: {}", remainingNodes);
        long usedSpace = getSelectedUsage(selectedNodes);
        long remainingSpace = getRemainingSpace(remainingNodes, dfsSpace);
        long safetyUsedSpace = ((Double) (usedSpace * replication * SAFETY_PERCENTAGE)).longValue();
        LOGGER.debug("Checking DFS space for decommission, usedSpace: {}, remainingSpace: {}", usedSpace, remainingSpace);
        LOGGER.debug("Used space with replication: {} and safety space: {} is: {}", replication, SAFETY_PERCENTAGE, safetyUsedSpace);
        if (remainingSpace < safetyUsedSpace) {
            throw new BadRequestException(
                    String.format("Trying to move '%s' bytes worth of data to nodes with '%s' bytes of capacity is not allowed", usedSpace, remainingSpace)
            );
        }

        ambariDecommissionTimeCalculator.calculateDecommissioningTime(stack, filteredHostList, dfsSpace, usedSpace);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_SELECT_FOR_DOWNSCALE, AVAILABLE.name(), selectedNodes.keySet());
        return convert(selectedNodes, filteredHostList);
    }

    private Map<String, Map<Long, Long>> getDFSSpace(Stack stack, AmbariClient client) {
        AmbariDFSSpaceRetrievalTask dfsSpaceTask = new AmbariDFSSpaceRetrievalTask();
        PollingResult result = ambariClientPollingService.pollWithTimeoutSingleFailure(dfsSpaceTask, new AmbariClientPollerObject(stack, client),
                AmbariDFSSpaceRetrievalTask.AMBARI_RETRYING_INTERVAL, AmbariDFSSpaceRetrievalTask.AMBARI_RETRYING_COUNT);
        if (result == SUCCESS) {
            return dfsSpaceTask.getDfsSpace();
        } else {
            throw new CloudbreakServiceException("Failed to get dfs space from ambari!");
        }
    }

    private Map<String, Long> removeSelected(Map<String, Long> all, Map<String, Long> selected) {
        Map<String, Long> copy = new HashMap<>(all);
        for (String host : selected.keySet()) {
            Iterator<String> iterator = copy.keySet().iterator();
            while (iterator.hasNext()) {
                if (iterator.next().equalsIgnoreCase(host)) {
                    iterator.remove();
                    break;
                }
            }
        }
        return copy;
    }

    private long getSelectedUsage(Map<String, Long> selected) {
        long usage = 0;
        for (Long hostUsage : selected.values()) {
            usage += hostUsage;
        }
        return usage;
    }

    private long getRemainingSpace(Map<String, Long> remainingNodes, Map<String, Map<Long, Long>> dfsSpace) {
        long remaining = 0;
        for (String host : remainingNodes.keySet()) {
            Map<Long, Long> space = dfsSpace.get(host);
            remaining += space.keySet().iterator().next();
        }
        return remaining;
    }

    private List<HostMetadata> convert(Map<String, Long> selectedNodes, Collection<HostMetadata> filteredHostList) {
        return filteredHostList.stream()
                .filter(a -> selectedNodes.keySet().contains(a.getHostName()))
                .collect(Collectors.toList());
    }

    public PollingResult removeHostsFromOrchestrator(Stack stack, List<String> hostNames) throws CloudbreakException {
        LOGGER.debug("Remove hosts from orchestrator: {}", hostNames);
        Orchestrator orchestrator = stack.getOrchestrator();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
        try {
            LOGGER.debug("Orchestrator type: {}", orchestratorType);
            if (orchestratorType.containerOrchestrator()) {
                Map<String, Object> map = new HashMap<>(orchestrator.getAttributes().getMap());
                OrchestrationCredential credential = new OrchestrationCredential(orchestrator.getApiEndpoint(), map);
                ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get(orchestrator.getType());
                Set<Container> containers = containerRepository.findContainersInCluster(stack.getCluster().getId());
                List<ContainerInfo> containersToDelete = containers.stream()
                        .filter(input -> hostNames.contains(input.getHost()) && input.getImage().contains(AMBARI_AGENT.getName()))
                        .map(input -> new ContainerInfo(input.getContainerId(), input.getName(), input.getHost(), input.getImage()))
                        .collect(Collectors.toList());
                containerOrchestrator.deleteContainer(containersToDelete, credential);
                containerRepository.deleteAll(containers);
                return waitForHostsToLeave(stack, hostNames);
            } else if (orchestratorType.hostOrchestrator()) {
                HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
                Map<String, String> privateIpsByFQDN = new HashMap<>();
                stack.getInstanceMetaDataAsList().stream()
                        .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                        .filter(instanceMetaData ->
                                hostNames.stream()
                                        .anyMatch(hn -> hn.equals(instanceMetaData.getDiscoveryFQDN())))
                        .forEach(instanceMetaData -> privateIpsByFQDN.put(instanceMetaData.getDiscoveryFQDN(), instanceMetaData.getPrivateIp()));
                List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
                hostOrchestrator.tearDown(allGatewayConfigs, privateIpsByFQDN);
            }
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.info("Failed to delete orchestrator components while decommissioning: ", e);
            throw new CloudbreakException("Failed to delete orchestrator components while decommissioning: ", e);
        }
        return SUCCESS;
    }

    private PollingResult waitForHostsToLeave(Stack stack, List<String> hostNames) {
        AmbariClient ambariClient = getAmbariClient(stack);
        return ambariHostLeave.pollWithTimeout(hostsLeaveStatusCheckerTask, new AmbariHostsWithNames(stack, ambariClient, hostNames),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_HOSTS, AmbariOperationService.MAX_FAILURE_COUNT).getLeft();
    }

    private PollingResult waitForDataNodeDecommission(Stack stack, AmbariClient ambariClient, Collection<String> hosts,
            Map<String, Map<String, String>> runningComponents) {
        if (hosts.stream().noneMatch(hn -> runningComponents.get(hn).keySet().contains(DATANODE))) {
            return SUCCESS;
        }

        LOGGER.debug("Waiting for DataNodes to move the blocks to other nodes. stack id: {}", stack.getId());
        return ambariOperationService.waitForOperations(stack, ambariClient, dnDecommissionStatusCheckerTask, Collections.emptyMap(),
                DECOMMISSION_SERVICES_AMBARI_PROGRESS_STATE).getLeft();
    }

    private PollingResult waitForRegionServerDecommission(Stack stack, AmbariClient ambariClient, List<String> hosts,
            Map<String, Map<String, String>> runningComponents) {
        if (COMPONENTS_NEED_TO_DECOMMISSION.get("HBASE_REGIONSERVER") == null
                || hosts.stream().noneMatch(hn -> runningComponents.get(hn).keySet().contains("HBASE_REGIONSERVER"))) {
            return SUCCESS;
        }

        LOGGER.debug("Waiting for RegionServers to move the regions to other servers");
        return rsPollerService.pollWithTimeoutSingleFailure(rsDecommissionStatusCheckerTask, new AmbariHostsWithNames(stack, ambariClient, hosts),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_REGION_DECOM);
    }

    private Set<String> selectHostsToRemove(List<HostMetadata> decommissionCandidates, int adjustment) {
        Stream<HostMetadata> orderedByHealth = decommissionCandidates.stream().sorted((a, b) -> {
            if (a.getHostMetadataState().equals(b.getHostMetadataState())) {
                return 0;
            } else if (!HEALTHY.equals(a.getHostMetadataState())) {
                return 1;
            } else {
                return -1;
            }
        });
        Set<String> hostsToRemove = orderedByHealth.map(HostMetadata::getHostName).limit(adjustment).collect(Collectors.toSet());
        LOGGER.debug("Hosts '{}' will be removed from Ambari cluster", hostsToRemove);
        return hostsToRemove;
    }

    private Map<String, Integer> decommissionComponents(AmbariClient ambariClient, Collection<String> hosts,
            Map<String, Map<String, String>> runningComponents, Stack stack) {
        Map<String, Integer> decommissionRequests = new HashMap<>();
        List<String> instances = stack.getInstanceMetaDataAsList().stream()
                .filter(instanceMetaData -> hosts.contains(instanceMetaData.getDiscoveryFQDN()))
                .map(InstanceMetaData::getInstanceId)
                .collect(Collectors.toList());
        Optional<Resource> volumeSet = stack.getDiskResources().stream()
                .filter(resource -> instances.contains(resource.getInstanceId()))
                .findFirst();
        Boolean decomissionVolumes = volumeSet
                .flatMap(resource -> stackUtil.getTypedAttributes(resource, VolumeSetAttributes.class))
                .map(VolumeSetAttributes::getDeleteOnTermination)
                .orElse(Boolean.TRUE);

        COMPONENTS_NEED_TO_DECOMMISSION.keySet().forEach(component -> {
            if (!decomissionVolumes) {
                return;
            }

            List<String> hostsRunService = hosts.stream().filter(hn -> runningComponents.get(hn).keySet().contains(component)).collect(Collectors.toList());
            Function<List<String>, Integer> action;
            switch (component) {
                case "NODEMANAGER":
                    action = ambariClient::decommissionNodeManagers;
                    break;
                case DATANODE:
                    action = ambariClient::decommissionDataNodes;
                    break;
                case "HBASE_REGIONSERVER":
                    action = l -> {
                        ambariClient.setHBaseRegionServersToMaintenance(l, true);
                        return ambariClient.decommissionHBaseRegionServers(l);
                    };
                    break;
                default:
                    throw new UnsupportedOperationException("Component decommission not allowed: " + component);
            }
            Integer requestId = decommissionComponent(ambariClient, hostsRunService, component, action);
            if (requestId != null) {
                decommissionRequests.put(component + "_DECOMMISSION", requestId);
            }
        });
        return decommissionRequests;
    }

    private Integer decommissionComponent(ServiceAndHostService ambariClient, Collection<String> hosts, String component,
            Function<List<String>, Integer> action) {
        List<String> hostsToDecommission = hosts.stream()
                .filter(h -> "INSERVICE".equals(ambariClient.getComponentStates(h, component).get("desired_admin_state"))).collect(Collectors.toList());
        if (!hostsToDecommission.isEmpty()) {
            return action.apply(hostsToDecommission);
        }
        return null;
    }

    private PollingResult stopHadoopComponents(Stack stack, AmbariClient ambariClient, List<String> hosts, Map<String, Map<String, String>> runningComponents) {
        try {
            hosts = hosts.stream()
                    .filter(hn -> !runningComponents.get(hn).isEmpty()).collect(Collectors.toList());
            if (!hosts.isEmpty()) {
                int requestId = ambariClient.stopAllComponentsOnHosts(hosts);
                return ambariOperationService.waitForOperations(stack, ambariClient, singletonMap("Stopping components on the decommissioned hosts", requestId),
                        STOP_SERVICES_AMBARI_PROGRESS_STATE).getLeft();
            }
            return SUCCESS;
        } catch (HttpResponseException e) {
            String errorMessage = AmbariClientExceptionUtil.getErrorMessage(e);
            throw new AmbariOperationFailedException("Ambari could not stop components. " + errorMessage, e);
        }
    }

    private PollingResult startServicesIfNeeded(Stack stack, AmbariClient ambariClient, Map<String, Map<String, String>> runningComponents) {
        Map<String, Integer> requests = new HashMap<>();
        try {
            for (String service : collectServicesToStart(ambariClient, runningComponents)) {
                int requestId = ambariClient.startService(service);
                if (requestId != -1) {
                    requests.put(service + "_START", requestId);
                }
            }
        } catch (Exception e) {
            LOGGER.info("Failed to start HDFS/YARN/HBASE services", e);
            throw new BadRequestException("Failed to start the HDFS, YARN and HBASE services, it's possible that some of the nodes are unavailable");
        }

        return requests.isEmpty() ? SUCCESS
                : ambariOperationService.waitForOperations(stack, ambariClient, requests, START_SERVICES_AMBARI_PROGRESS_STATE).getLeft();
    }

    private Iterable<String> collectServicesToStart(ServiceAndHostService ambariClient, Map<String, Map<String, String>> runningComponents) {
        Collection<String> services = new HashSet<>();
        for (Entry<String, Map<String, String>> hostComponentsEntry : runningComponents.entrySet()) {
            for (Entry<String, String> componentStateEntry : hostComponentsEntry.getValue().entrySet()) {
                String component = componentStateEntry.getKey();
                if (!"STARTED".equals(componentStateEntry.getValue()) && COMPONENTS_NEED_TO_DECOMMISSION.keySet().contains(component)) {
                    Map<String, String> componentStates = ambariClient.getComponentStates(hostComponentsEntry.getKey(), component);
                    if ("DECOMMISSIONED".equals(componentStates.get("desired_admin_state"))) {
                        LOGGER.debug("No need to start ambari service {} on host {}", component, hostComponentsEntry.getKey());
                    } else {
                        services.add(COMPONENTS_NEED_TO_DECOMMISSION.get(component));
                    }
                }
            }
        }
        return services;
    }

    private void calculateDecommissioningTime(Stack stack, Collection<HostMetadata> filteredHostList, AmbariClient ambariClient) {
        Map<String, Map<Long, Long>> dfsSpace = getDFSSpace(stack, ambariClient);
        Map<String, Long> sortedAscending = sortByUsedSpace(dfsSpace, false);
        Map<String, Long> usedSpaceByHostname = sortedAscending.entrySet().stream()
                .filter(entry -> filteredHostList.stream().anyMatch(hm -> hm.getHostName().equalsIgnoreCase(entry.getKey())))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        long usedSpace = getSelectedUsage(usedSpaceByHostname);
        ambariDecommissionTimeCalculator.calculateDecommissioningTime(stack, filteredHostList, dfsSpace, usedSpace);
    }
}
