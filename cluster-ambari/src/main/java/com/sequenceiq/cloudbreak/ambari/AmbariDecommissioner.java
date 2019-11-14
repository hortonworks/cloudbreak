package com.sequenceiq.cloudbreak.ambari;

import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.DECOMMISSION_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.DECOMMISSION_SERVICES_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.START_SERVICES_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.STOP_SERVICES_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.ambari.DataNodeUtils.sortByUsedSpace;
import static com.sequenceiq.cloudbreak.ambari.flow.AmbariOperationService.AMBARI_POLLING_INTERVAL;
import static com.sequenceiq.cloudbreak.ambari.flow.AmbariOperationService.MAX_ATTEMPTS_FOR_HOSTS;
import static com.sequenceiq.cloudbreak.polling.PollingResult.SUCCESS;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isSuccess;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.net.URISyntaxException;
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

import com.google.common.collect.Sets;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.model.HostComponentStatuses;
import com.sequenceiq.ambari.client.model.HostStatus;
import com.sequenceiq.ambari.client.services.ServiceAndHostService;
import com.sequenceiq.cloudbreak.ambari.filter.HostFilterService;
import com.sequenceiq.cloudbreak.ambari.flow.AmbariClientPollerObject;
import com.sequenceiq.cloudbreak.ambari.flow.AmbariDFSSpaceRetrievalTask;
import com.sequenceiq.cloudbreak.ambari.flow.AmbariHostsLeaveStatusCheckerTask;
import com.sequenceiq.cloudbreak.ambari.flow.AmbariHostsWithNames;
import com.sequenceiq.cloudbreak.ambari.flow.AmbariOperationService;
import com.sequenceiq.cloudbreak.ambari.flow.DNDecommissionStatusCheckerTask;
import com.sequenceiq.cloudbreak.ambari.flow.RSDecommissionStatusCheckerTask;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.service.DecommissionException;
import com.sequenceiq.cloudbreak.cluster.service.NotEnoughNodeException;
import com.sequenceiq.cloudbreak.cluster.service.NotRecommendedNodeRemovalException;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.filter.ConfigParam;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.OperationException;

import groovyx.net.http.HttpResponseException;

@Component
public class AmbariDecommissioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariDecommissioner.class);

    private static final int MAX_ATTEMPTS_FOR_REGION_DECOM = 500;

    private static final String DATANODE = "DATANODE";

    private static final String HBASE_REGIONSERVER = "HBASE_REGIONSERVER";

    private static final String NODEMANAGER = "NODEMANAGER";

    private static final double SAFETY_PERCENTAGE = 1.2;

    private static final int NO_REPLICATION = 0;

    private static final Map<String, String> COMPONENTS_NEED_TO_DECOMMISSION = new HashMap<>();

    @Inject
    private AmbariClientFactory ambariClientFactory;

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
    private AmbariDeleteHostsService ambariDeleteHostsService;

    @Inject
    private HostFilterService hostFilterService;

    @Inject
    private AmbariDecommissionTimeCalculator ambariDecommissionTimeCalculator;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @PostConstruct
    public void init() {
        COMPONENTS_NEED_TO_DECOMMISSION.put(DATANODE, "HDFS");
        COMPONENTS_NEED_TO_DECOMMISSION.put("NODEMANAGER", "YARN");
    }

    public Set<InstanceMetaData> collectDownscaleCandidates(AmbariClient ambariClient, Stack stack, HostGroup hostGroup, Integer scalingAdjustment)
            throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        int adjustment = Math.abs(scalingAdjustment);
        Set<InstanceMetaData> hostsToRemove =
                selectHostsToRemove(
                        collectDownscaleCandidatesFromCluster(ambariClient, stack, cluster, hostGroup.getName(), adjustment),
                        adjustment);
        if (hostsToRemove.size() != adjustment) {
            throw new CloudbreakException(String.format("Only %d hosts found to downscale but %d required.", hostsToRemove.size(), adjustment));
        }
        return hostsToRemove;
    }

    public Set<String> decommissionAmbariNodes(Stack stack, Map<String, InstanceMetaData> hostsToRemove, AmbariClient ambariClient)
            throws IOException, URISyntaxException {
        LOGGER.info("Decommission ambari nodes: {}", hostsToRemove);
        Map<String, HostStatus> hostStatusMap = ambariClient.getHostsStatuses(new ArrayList<>(hostsToRemove.keySet()));
        LOGGER.info("Host status map for decommission: {}", hostStatusMap);
        Map<String, InstanceMetaData> unhealthyHosts = new HashMap<>();
        Map<String, InstanceMetaData> healthyHosts = new HashMap<>();
        for (Entry<String, InstanceMetaData> hostToRemove : hostsToRemove.entrySet()) {
            if (hostStatusMap.get(hostToRemove.getKey()) == null || "UNKNOWN".equals(hostStatusMap.get(hostToRemove.getKey()).getHostStatus())) {
                unhealthyHosts.put(hostToRemove.getKey(), hostToRemove.getValue());
            } else {
                healthyHosts.put(hostToRemove.getKey(), hostToRemove.getValue());
            }
        }

        LOGGER.info("Unhealthy hosts: {}", unhealthyHosts);
        LOGGER.info("Healthy hosts: {}", healthyHosts);

        Set<String> deletedHosts = new HashSet<>();
        if (!unhealthyHosts.isEmpty()) {
            LOGGER.info("Unhealthy hosts are not empty, lets delete them first");
            for (Entry<String, InstanceMetaData> host : unhealthyHosts.entrySet()) {
                InstanceMetaData instanceMetaData = host.getValue();
                deleteHostFromAmbariIfInUnknownState(instanceMetaData, hostStatusMap, ambariClient);
                instanceMetaData.setInstanceStatus(InstanceStatus.FAILED);
                instanceMetaData.setStatusReason("Instance is in UNKNOWN state in Ambari");
                deletedHosts.add(host.getKey());
            }
        }

        LOGGER.info("Deleted hosts, after unhealthy hosts removal process: {}", deletedHosts);

        if (!healthyHosts.isEmpty()) {
            LOGGER.info("Healthy hosts are not empty, delete them also");
            deletedHosts.addAll(decommissionAmbariNodes(stack, healthyHosts, hostStatusMap, ambariClient));
        }

        LOGGER.info("Deleted hosts are: {}", deletedHosts);
        return deletedHosts;
    }

    public boolean deleteHostFromAmbariIfInUnknownState(InstanceMetaData data, AmbariClient ambariClient) throws IOException, URISyntaxException {
        LOGGER.info("Delete host from ambari {}", data);
        Map<String, HostStatus> hostStatusMap = ambariClient.getHostsStatuses(List.of(data.getDiscoveryFQDN()));
        return deleteHostFromAmbariIfInUnknownState(data, hostStatusMap, ambariClient);
    }

    private boolean deleteHostFromAmbariIfInUnknownState(InstanceMetaData instanceMetaData, Map<String, HostStatus> hostsStatuses, AmbariClient ambariClient)
            throws IOException, URISyntaxException {
        LOGGER.info("Delete host from ambari: {}", instanceMetaData.getInstanceName());
        boolean hostDeleted = false;
        if (hostsStatuses.containsKey(instanceMetaData.getDiscoveryFQDN())) {
            HostStatus hostStatus = hostsStatuses.get(instanceMetaData.getDiscoveryFQDN());
            String hostState = hostStatus.getHostStatus();
            if ("UNKNOWN".equals(hostState)) {
                LOGGER.info("Host is in UNKNOWN state, lets delete: {}", instanceMetaData.getInstanceName());
                ambariDeleteHostsService.deleteHostsButFirstQueryThemFromAmbari(ambariClient, singletonList(instanceMetaData.getDiscoveryFQDN()));
                hostDeleted = true;
            }
        } else {
            LOGGER.info("Host is already deleted. {}", instanceMetaData.getInstanceName());
            hostDeleted = true;
        }
        return hostDeleted;
    }

    private AmbariClient getAmbariClient(Stack stack, HttpClientConfig clientConfig) {
        return ambariClientFactory.getAmbariClient(stack, stack.getCluster(), clientConfig);
    }

    public Map<String, InstanceMetaData> collectHostsToRemove(Stack stack, String groupName, Set<String> hostNames, AmbariClient ambariClient) {
        LOGGER.info("Collect hosts from '{}' to remove: '{}'", groupName, hostNames);
        Optional<InstanceGroup> instanceGroupForHostGroup = stack.getInstanceGroups().stream()
                .filter(instanceGroup -> instanceGroup.getGroupName().equals(groupName))
                .findFirst();
        if (instanceGroupForHostGroup.isPresent()) {
            Map<String, InstanceMetaData> hostsToRemove = collectInstanceMetaDatas(instanceGroupForHostGroup.get(), hostNames);
            if (hostsToRemove.size() != hostNames.size()) {
                LOGGER.warn("Not all the hosts found in the given host group.");
            }
            List<String> runningHosts = ambariClient.getClusterHosts();
            LOGGER.info("Running hosts: {}", runningHosts);
            if (runningHosts == null) {
                throw new OperationException("Running hosts is null");
            }
            Sets.newHashSet(hostsToRemove.keySet()).forEach(hostName -> {
                if (!runningHosts.contains(hostName)) {
                    hostsToRemove.remove(hostName);
                }
            });
            LOGGER.info("Hosts to remove: {}", hostsToRemove);
            return hostsToRemove;
        } else {
            throw new CloudbreakServiceException("Can not find instance group by name: " + groupName);
        }
    }

    private Map<String, InstanceMetaData> collectInstanceMetaDatas(InstanceGroup instanceGroup, Collection<String> hostNames) {
        LOGGER.info("Collect host metadata, instancegroup: {}, hostnames: {}", instanceGroup, hostNames);
        return instanceGroup.getInstanceMetaDataSet().stream().filter(instanceMetaData -> hostNames.contains(instanceMetaData.getDiscoveryFQDN())).collect(
                toMap(InstanceMetaData::getDiscoveryFQDN, hostMetadata -> hostMetadata));
    }

    public Map<String, Long> selectNodes(Map<String, Long> sortedAscending, Collection<InstanceMetaData> filteredHostList, int removeCount) {
        LOGGER.info("sortedAscending: {}, filteredHostList: {}", sortedAscending, filteredHostList);

        Map<String, Long> select = filteredHostList
                .stream()
                .filter(instanceMetaData -> instanceMetaData.isUnhealthy() || instanceMetaData.isDeletedOnProvider())
                .limit(removeCount)
                .collect(toMap(InstanceMetaData::getDiscoveryFQDN, o -> 0L));

        LOGGER.info("Selected UNHEALTHY nodes: {}", select);

        if (select.size() < removeCount) {
            LOGGER.info("Selected nodes size smaller than remove count, add some nodes");
            Set<String> hostNames = filteredHostList.stream().map(a -> a.getDiscoveryFQDN().toLowerCase()).collect(Collectors.toSet());
            sortedAscending.entrySet().stream()
                    .filter(entry -> !select.keySet().contains(entry.getKey()) && hostNames.contains(entry.getKey().toLowerCase()))
                    .limit(removeCount - select.size())
                    .forEach(entry -> select.put(entry.getKey(), entry.getValue()));
        }
        LOGGER.info("Selected nodes to remove: {}", removeCount);
        return select;
    }

    private Collection<String> decommissionAmbariNodes(Stack stack, Map<String, InstanceMetaData> hostsToRemove,
            Map<String, HostStatus> hostStatusMap, AmbariClient ambariClient) {
        Collection<String> result = new HashSet<>();
        PollingResult pollingResult = startServicesIfNeeded(stack, ambariClient, hostStatusMap);
        try {
            if (isSuccess(pollingResult)) {
                LOGGER.info("All services started, let's decommission");
                List<String> hostList = new ArrayList<>(hostsToRemove.keySet());
                Map<String, Integer> decommissionRequests = decommissionComponents(ambariClient, hostList, hostStatusMap);
                if (!decommissionRequests.isEmpty()) {
                    pollingResult =
                            ambariOperationService.waitForOperations(stack, ambariClient, decommissionRequests, DECOMMISSION_AMBARI_PROGRESS_STATE).getLeft();
                }
                if (!isSuccess(pollingResult)) {
                    Map<String, HostStatus> statuses = ambariClient.getHostComponentStatuses(hostList, List.of(NODEMANAGER));
                    throw new DecommissionException("Nodemanager could not be decommissioned on hosts, current states " + statuses);
                }
                pollingResult = waitForDataNodeDecommission(stack, ambariClient, hostList, hostStatusMap);
                if (!isSuccess(pollingResult)) {
                    Map<String, HostStatus> statuses = ambariClient.getHostComponentStatuses(hostList, List.of(DATANODE));
                    throw new DecommissionException("Datanode could not be decommissioned on hosts, current states " + statuses);
                }

                pollingResult = waitForRegionServerDecommission(stack, ambariClient, hostList, hostStatusMap);
                if (!isSuccess(pollingResult)) {
                    Map<String, HostStatus> statuses = ambariClient.getHostComponentStatuses(hostList, List.of(HBASE_REGIONSERVER));
                    throw new DecommissionException("HBASE region server could not be decommissioned on hosts, current states " + statuses);
                }
                pollingResult = stopHadoopComponents(stack, ambariClient, hostList, hostStatusMap);
                if (!isSuccess(pollingResult)) {
                    throw new DecommissionException("Hadoop components could not be stop on hosts");
                }
                ambariDeleteHostsService.deleteHostsButFirstQueryThemFromAmbari(ambariClient, hostList);
                result.addAll(hostsToRemove.keySet());
            }
        } catch (DecommissionException e) {
            throw e;
        } catch (Exception e) {
            throw new DecommissionException(e);
        }
        LOGGER.info("Decomissioned hosts: {}", result);
        return result;
    }

    private Map<String, Map<String, String>> getComponentStatusesForHostes(AmbariClient ambariClient, List<String> hostList, String component) {
        return hostList.stream().collect(toMap(k -> k, h -> ambariClient.getComponentStates(h, component)));
    }

    public Map<String, Map<String, Map<String, String>>> getStatusOfComponents(AmbariClient ambariClient, Collection<String> hosts) {
        return hosts.stream().collect(toMap(k -> k, host -> getStatusOfComponentsForHost(ambariClient, host)));
    }

    public Map<String, Map<String, String>> getStatusOfComponentsForHost(AmbariClient ambariClient, String host) {
        return ambariClient.getHostComponentsMap(host).keySet()
                .stream()
                .filter(s -> COMPONENTS_NEED_TO_DECOMMISSION.keySet().contains(s))
                .collect(toMap(k -> k, v -> ambariClient.getComponentStates(host, v)));
    }

    private void deleteHosts(Iterable<String> hosts, Map<String, Map<String, String>> components, ServiceAndHostService client)
            throws IOException, URISyntaxException {
        for (String hostName : hosts) {
            client.deleteHostComponents(hostName, new ArrayList<>(components.get(hostName).keySet()));
            client.deleteHost(hostName);
        }
    }

    private List<InstanceMetaData> collectDownscaleCandidatesFromCluster(AmbariClient ambariClient, Stack stack, Cluster cluster, String groupName,
            Integer scalingAdjustment) {
        LOGGER.info("Collect downscale candidates from cluster. hostgroup: {}, scalingAdjustment: {}, forced: {}", groupName, scalingAdjustment);
        List<InstanceMetaData> downScaleCandidates;
        Optional<InstanceGroup> instanceGroupByGroupName = stack.getInstanceGroups().stream()
                .filter(instanceGroup -> instanceGroup.getGroupName().equals(groupName)).findFirst();
        if (instanceGroupByGroupName.isPresent()) {
            Set<InstanceMetaData> hostsInHostGroup = instanceGroupByGroupName.get().getAttachedInstanceMetaDataSet();
            LOGGER.info("Hosts in {} hostgroup: {}", groupName, hostsInHostGroup.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toList()));
            List<InstanceMetaData> filteredHostList = hostFilterService.filterHostsForDecommission(cluster, hostsInHostGroup, groupName, ambariClient);
            int reservedInstances = hostsInHostGroup.size() - filteredHostList.size();
            LOGGER.info("Reserved instances size: {}", reservedInstances);
            String blueprintName = cluster.getBlueprint().getStackName();
            Map<String, List<String>> blueprintMap = ambariClient.getBlueprintMap(blueprintName);
            LOGGER.info("Blueprint map: {}", blueprintMap);
            if (hostGroupNodesAreDataNodes(blueprintMap, groupName)) {
                LOGGER.info("Hostgroup nodes are datanodes");
                int replication = getReplicationFactor(ambariClient, groupName);
                LOGGER.info("Replication is: {}", replication);
                verifyNodeCount(replication, scalingAdjustment, filteredHostList.size(), reservedInstances);
                downScaleCandidates = checkAndSortByAvailableSpace(stack, ambariClient, replication, scalingAdjustment, filteredHostList);
            } else {
                LOGGER.info("Hostgroup nodes are not datanodes, but verify node count");
                verifyNodeCount(NO_REPLICATION, scalingAdjustment, filteredHostList.size(), reservedInstances);
                downScaleCandidates = filteredHostList;
            }
            LOGGER.info("Downscale candidates are: {}", downScaleCandidates);
            return downScaleCandidates;
        } else {
            throw new CloudbreakServiceException("Can not find instance group by name: " + groupName);
        }
    }

    private boolean hostGroupNodesAreDataNodes(Map<String, List<String>> blueprintMap, String hostGroupName) {
        return blueprintMap.get(hostGroupName).contains(DATANODE);
    }

    public void verifyNodesAreRemovable(Stack stack, Collection<InstanceMetaData> removableInstanceMetaDataList, AmbariClient ambariClient) {
        LOGGER.info("Verify nodes are removable {}", removableInstanceMetaDataList);
        Cluster cluster = stack.getCluster();
        String blueprintName = cluster.getBlueprint().getStackName();
        Map<String, List<String>> blueprintMap = ambariClient.getBlueprintMap(blueprintName);
        LOGGER.info("Blueprint map for verifying: {}", blueprintMap);

        Set<InstanceGroup> instanceGroups = stack.getInstanceGroups();
        for (InstanceGroup instanceGroup : instanceGroups) {
            Set<InstanceMetaData> attachedInstanceMetaDataSet = instanceGroup.getAttachedInstanceMetaDataSet();
            List<InstanceMetaData> removableAttachedInstanceMetaData = attachedInstanceMetaDataSet.stream()
                    .filter(instanceMetaData -> removableInstanceMetaDataList.stream()
                            .map(InstanceMetaData::getPrivateId)
                            .anyMatch(removablePrivateId -> removablePrivateId.equals(instanceMetaData.getPrivateId())))
                    .collect(Collectors.toList());
            if (!removableAttachedInstanceMetaData.isEmpty()) {
                List<InstanceMetaData> hostListForDecommission =
                        hostFilterService.filterHostsForDecommission(cluster, removableAttachedInstanceMetaData, instanceGroup.getGroupName(),
                                ambariClient);
                LOGGER.info("Host list for decommission: {}", hostListForDecommission);
                boolean hostGroupContainsDatanode = hostGroupNodesAreDataNodes(blueprintMap, instanceGroup.getGroupName());
                int replication = hostGroupContainsDatanode ? getReplicationFactor(ambariClient, instanceGroup.getGroupName()) : NO_REPLICATION;
                LOGGER.info("Replication is {}", replication);
                if (hostListForDecommission.size() < removableAttachedInstanceMetaData.size()) {
                    LOGGER.info("Removable hosts size is bigger than host list for decommission");
                    List<InstanceMetaData> notRecommendedRemovableNodes = removableAttachedInstanceMetaData.stream()
                            .filter(hostMetadata -> !hostListForDecommission.contains(hostMetadata))
                            .collect(Collectors.toList());
                    throw new NotRecommendedNodeRemovalException("Following nodes shouldn't be removed from the cluster: "
                            + notRecommendedRemovableNodes.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toList()));
                }
                verifyNodeCount(replication, removableAttachedInstanceMetaData.size(), instanceGroup.getAttachedInstanceMetaDataSet().size(), 0);
                if (hostGroupContainsDatanode) {
                    calculateDecommissioningTime(stack, hostListForDecommission, ambariClient);
                }
            }
        }
    }

    private int getReplicationFactor(ServiceAndHostService ambariClient, String hostGroup) {
        Map<String, String> configuration = configurationService.getConfiguration(ambariClient, hostGroup);
        return Integer.parseInt(configuration.get(ConfigParam.DFS_REPLICATION.key()));
    }

    private void verifyNodeCount(int replication, int scalingAdjustment, int hostSize, int reservedInstances) {
        int adjustment = Math.abs(scalingAdjustment);
        if (hostSize + reservedInstances - adjustment < replication || hostSize < adjustment) {
            LOGGER.info("Cannot downscale: replication: {}, adjustment: {}, filtered host size: {}", replication, scalingAdjustment, hostSize);
            throw new NotEnoughNodeException("There is not enough node to downscale. "
                    + "Check the replication factor and the ApplicationMaster occupation.");
        }
    }

    private List<InstanceMetaData> checkAndSortByAvailableSpace(Stack stack, AmbariClient client, int replication, int adjustment,
            List<InstanceMetaData> filteredHostList) {
        int removeCount = Math.abs(adjustment);
        LOGGER.info("removeCount: {}, replication: {}, filteredHostList size: {}, filteredHostList: {}",
                removeCount, replication, filteredHostList.size(), filteredHostList);
        Map<String, Map<Long, Long>> dfsSpace = getDFSSpace(stack, client);
        Map<String, Long> sortedAscending = sortByUsedSpace(dfsSpace, false);
        LOGGER.info("sortedAscending: {}", sortedAscending);
        Map<String, Long> selectedNodes = selectNodes(sortedAscending, filteredHostList, removeCount);
        Map<String, Long> remainingNodes = removeSelected(sortedAscending, selectedNodes);
        LOGGER.info("Selected nodes for decommission: {}", selectedNodes);
        LOGGER.info("Remaining nodes after decommission: {}", remainingNodes);
        long usedSpace = getSelectedUsage(selectedNodes);
        long remainingSpace = getRemainingSpace(remainingNodes, dfsSpace);
        long safetyUsedSpace = ((Double) (usedSpace * replication * SAFETY_PERCENTAGE)).longValue();
        LOGGER.info("Checking DFS space for decommission, usedSpace: {}, remainingSpace: {}", usedSpace, remainingSpace);
        LOGGER.info("Used space with replication: {} and safety space: {} is: {}", replication, SAFETY_PERCENTAGE, safetyUsedSpace);
        if (remainingSpace < safetyUsedSpace) {
            throw new BadRequestException(
                    String.format("Trying to move '%s' bytes worth of data to nodes with '%s' bytes of capacity is not allowed", usedSpace, remainingSpace)
            );
        }

        ambariDecommissionTimeCalculator.calculateDecommissioningTime(stack, filteredHostList, dfsSpace, usedSpace, 0);
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

    private List<InstanceMetaData> convert(Map<String, Long> selectedNodes, Collection<InstanceMetaData> filteredHostList) {
        return filteredHostList.stream()
                .filter(a -> selectedNodes.keySet().contains(a.getDiscoveryFQDN()))
                .collect(Collectors.toList());
    }

    private PollingResult waitForHostsToLeave(Stack stack, List<String> hostNames, HttpClientConfig clientConfig) {
        AmbariClient ambariClient = getAmbariClient(stack, clientConfig);
        return ambariHostLeave.pollWithTimeout(hostsLeaveStatusCheckerTask, new AmbariHostsWithNames(stack, ambariClient, hostNames),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_HOSTS, AmbariOperationService.MAX_FAILURE_COUNT).getLeft();
    }

    private PollingResult waitForDataNodeDecommission(Stack stack, AmbariClient ambariClient, Collection<String> hosts,
            Map<String, HostStatus> hostStatusMap) {
        if (hosts.stream().noneMatch(hn -> hostStatusMap.get(hn).getHostComponentsStatuses().keySet().contains(DATANODE))) {
            return SUCCESS;
        }

        LOGGER.info("Waiting for DataNodes to move the blocks to other nodes. stack id: {}", stack.getId());
        return ambariOperationService.waitForOperations(stack, ambariClient, dnDecommissionStatusCheckerTask, Collections.emptyMap(),
                DECOMMISSION_SERVICES_AMBARI_PROGRESS_STATE).getLeft();
    }

    private PollingResult waitForRegionServerDecommission(Stack stack, AmbariClient ambariClient, List<String> hosts,
            Map<String, HostStatus> hostStatusMap) {
        if (COMPONENTS_NEED_TO_DECOMMISSION.get(HBASE_REGIONSERVER) == null
                || hosts.stream().noneMatch(hn -> hostStatusMap.get(hn).getHostComponentsStatuses().keySet().contains(HBASE_REGIONSERVER))) {
            return SUCCESS;
        }

        LOGGER.info("Waiting for RegionServers to move the regions to other servers");
        return rsPollerService.pollWithTimeoutSingleFailure(rsDecommissionStatusCheckerTask, new AmbariHostsWithNames(stack, ambariClient, hosts),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_REGION_DECOM);
    }

    private Set<InstanceMetaData> selectHostsToRemove(List<InstanceMetaData> decommissionCandidates, int adjustment) {
        LOGGER.info("Adjustment is {}, select hosts to remove from decommission candidates: {}", adjustment, decommissionCandidates);
        Stream<InstanceMetaData> orderedByHealth = decommissionCandidates.stream().sorted((a, b) -> {
            if (a.getInstanceStatus().equals(b.getInstanceStatus())) {
                return 0;
            } else if (!InstanceStatus.SERVICES_HEALTHY.equals(a.getInstanceStatus())) {
                return 1;
            } else {
                return -1;
            }
        });
        Set<InstanceMetaData> hostsToRemove = orderedByHealth.limit(adjustment).collect(Collectors.toSet());
        LOGGER.info("Hosts '{}' will be removed from Ambari cluster", hostsToRemove);
        return hostsToRemove;
    }

    private Map<String, Integer> decommissionComponents(AmbariClient ambariClient, Collection<String> hosts,
            Map<String, HostStatus> hostStatusMap) {
        LOGGER.info("Decommission components: {}, host status map: {}", hosts, hostStatusMap);
        Map<String, Integer> decommissionRequests = new HashMap<>();
        COMPONENTS_NEED_TO_DECOMMISSION.keySet().forEach(component -> {
            LOGGER.info("Component to decommission: {}", component);
            List<String> hostsRunService = hosts.stream().filter(hn -> hostStatusMap.get(hn).getHostComponentsStatuses().containsKey(component))
                    .collect(Collectors.toList());
            LOGGER.info("Hosts run this component: {}", hostsRunService);
            Function<List<String>, Integer> action;
            switch (component) {
                case NODEMANAGER:
                    action = h -> {
                        try {
                            return ambariClient.decommissionNodeManagers(h);
                        } catch (URISyntaxException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    };
                    break;
                case DATANODE:
                    action = h -> {
                        try {
                            return ambariClient.decommissionDataNodes(h);
                        } catch (URISyntaxException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    };
                    break;
                case HBASE_REGIONSERVER:
                    action = l -> {
                        try {
                            ambariClient.setHBaseRegionServersToMaintenance(l, true);
                            return ambariClient.decommissionHBaseRegionServers(l);
                        } catch (URISyntaxException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    };
                    break;
                default:
                    throw new UnsupportedOperationException("Component decommission not allowed: " + component);
            }
            Integer requestId = decommissionComponent(hostsRunService, component, hostStatusMap, action);
            if (requestId != null) {
                decommissionRequests.put(component + "_DECOMMISSION", requestId);
            }
        });
        LOGGER.info("Collected decommission requests: {}", decommissionRequests);
        return decommissionRequests;
    }

    private Integer decommissionComponent(Collection<String> hosts, String component, Map<String, HostStatus> hostStatusMap,
            Function<List<String>, Integer> action) {
        LOGGER.info("Decommission component {}, hosts {}, host status map {}", component, hosts, hostStatusMap);
        List<String> hostsToDecommission = hosts.stream()
                .filter(h -> "INSERVICE".equals(hostStatusMap.get(h).getHostComponentsStatuses().get(component).getDesired_admin_state()))
                .collect(Collectors.toList());
        LOGGER.info("Hosts to decommission for component: {}", hostsToDecommission);
        if (!hostsToDecommission.isEmpty()) {
            return action.apply(hostsToDecommission);
        }
        return null;
    }

    private PollingResult stopHadoopComponents(Stack stack, AmbariClient ambariClient, List<String> hosts, Map<String, HostStatus> hostStatusMap) {
        try {
            hosts = hosts.stream()
                    .filter(hn -> !hostStatusMap.get(hn).getHostComponentsStatuses().isEmpty()).collect(Collectors.toList());
            if (!hosts.isEmpty()) {
                int requestId = ambariClient.stopAllComponentsOnHosts(hosts);
                return ambariOperationService.waitForOperations(stack, ambariClient, singletonMap("Stopping components on the decommissioned hosts", requestId),
                        STOP_SERVICES_AMBARI_PROGRESS_STATE).getLeft();
            }
            return SUCCESS;
        } catch (HttpResponseException e) {
            String errorMessage = AmbariClientExceptionUtil.getErrorMessage(e);
            throw new AmbariOperationFailedException("Ambari could not stop components. " + errorMessage, e);
        } catch (URISyntaxException | IOException e) {
            throw new AmbariOperationFailedException("Ambari could not stop components. " + e.getMessage(), e);
        }
    }

    private PollingResult startServicesIfNeeded(Stack stack, AmbariClient ambariClient, Map<String, HostStatus> hostStatusMap) {
        LOGGER.info("Start services if needed on hosts: {}", hostStatusMap);
        Map<String, Integer> requests = new HashMap<>();
        try {
            for (String service : collectServicesToStart(hostStatusMap)) {
                int requestId = ambariClient.startService(service);
                if (requestId != -1) {
                    requests.put(service + "_START", requestId);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to start HDFS/YARN/HBASE services", e);
            throw new BadRequestException("Failed to start the HDFS, YARN and HBASE services, it's possible that some of the nodes are unavailable");
        }

        if (requests.isEmpty()) {
            LOGGER.info("No need to start any services");
            return SUCCESS;
        } else {
            LOGGER.info("We need to start services, lets wait, requests: {}", requests);
            return ambariOperationService.waitForOperations(stack, ambariClient, requests, START_SERVICES_AMBARI_PROGRESS_STATE).getLeft();
        }
    }

    private Iterable<String> collectServicesToStart(Map<String, HostStatus> hostStatusMap) {
        LOGGER.info("Collect services to start for hosts {}", hostStatusMap);
        Collection<String> services = new HashSet<>();
        for (Entry<String, HostStatus> hostStatusEntry : hostStatusMap.entrySet()) {
            for (Entry<String, HostComponentStatuses> componentStateEntry : hostStatusEntry.getValue().getHostComponentsStatuses().entrySet()) {
                String component = componentStateEntry.getKey();
                HostComponentStatuses hostComponentStatuses = componentStateEntry.getValue();
                if (!"STARTED".equals(hostComponentStatuses.getState()) && COMPONENTS_NEED_TO_DECOMMISSION.keySet().contains(component)) {
                    if ("DECOMMISSIONED".equals(hostComponentStatuses.getDesired_admin_state())) {
                        LOGGER.info("No need to start ambari service {} on host {}", component, hostStatusEntry.getKey());
                    } else {
                        services.add(COMPONENTS_NEED_TO_DECOMMISSION.get(component));
                    }
                }
            }
        }
        LOGGER.info("Collected services to start: {}", services);
        return services;
    }

    private void calculateDecommissioningTime(Stack stack, Collection<InstanceMetaData> filteredHostList, AmbariClient ambariClient) {
        LOGGER.info("Calculate decomissioning time for hosts: {}", filteredHostList);
        Map<String, Map<Long, Long>> dfsSpace = getDFSSpace(stack, ambariClient);
        Map<String, Long> sortedAscending = sortByUsedSpace(dfsSpace, false);
        Map<String, Long> usedSpaceByHostname = sortedAscending.entrySet().stream()
                .filter(entry -> filteredHostList.stream().anyMatch(hm -> hm.getDiscoveryFQDN().equalsIgnoreCase(entry.getKey())))
                .collect(toMap(Entry::getKey, Entry::getValue));
        long usedSpace = getSelectedUsage(usedSpaceByHostname);
        ambariDecommissionTimeCalculator.calculateDecommissioningTime(stack, filteredHostList, dfsSpace, usedSpace, 0);
    }
}
