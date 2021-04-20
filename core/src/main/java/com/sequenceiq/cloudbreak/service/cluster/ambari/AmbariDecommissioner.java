package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.common.type.HostMetadataState.HEALTHY;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_AGENT;
import static com.sequenceiq.cloudbreak.service.PollingResult.SUCCESS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.DECOMMISSION_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.DECOMMISSION_SERVICES_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.RETIRE_NIFI_NODE_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.START_SERVICES_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.STOP_SERVICES_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.DataNodeUtils.sortByUsedSpace;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.AMBARI_POLLING_INTERVAL;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.MAX_ATTEMPTS_FOR_HOSTS;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.model.HostComponentStatuses;
import com.sequenceiq.ambari.client.model.HostStatus;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
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
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientRetryer;
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

import groovyx.net.http.HttpResponseException;

@Component
public class AmbariDecommissioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariDecommissioner.class);

    private static final int MAX_ATTEMPTS_FOR_REGION_DECOM = 500;

    private static final String DATANODE = "DATANODE";

    private static final String HBASE_REGIONSERVER = "HBASE_REGIONSERVER";

    private static final String NODEMANAGER = "NODEMANAGER";

    private static final String NIFI = "NIFI_MASTER";

    private static final double SAFETY_PERCENTAGE = 1.2;

    private static final int NO_REPLICATION = 0;

    private static final Map<String, String> COMPONENTS_NEED_TO_DECOMMISSION = new HashMap<>();

    @Inject
    private AmbariClientRetryer ambariClientRetryer;

    @Inject
    private AmbariDeleteHostsService ambariDeleteHostsService;

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
    private TransactionService transactionService;

    @PostConstruct
    public void init() {
        COMPONENTS_NEED_TO_DECOMMISSION.put(DATANODE, "HDFS");
        COMPONENTS_NEED_TO_DECOMMISSION.put(NODEMANAGER, "YARN");
    }

    public Set<String> collectDownscaleCandidates(Stack stack, String hostGroupName, Integer scalingAdjustment, boolean forced) throws CloudbreakException {
        LOGGER.info("Collect downscale candidates. hostgroup: {}, scalingAdjustment: {}, forced: {}", hostGroupName, scalingAdjustment, forced);
        Cluster cluster = stack.getCluster();
        int adjustment = Math.abs(scalingAdjustment);
        Set<String> hostsToRemove = selectHostsToRemove(collectDownscaleCandidatesFromCluster(stack, cluster, hostGroupName, adjustment, forced), adjustment);
        if (hostsToRemove.size() != adjustment) {
            throw new CloudbreakException(String.format("Only %d hosts found to downscale but %d required.", hostsToRemove.size(), adjustment));
        }
        LOGGER.info("{} will be removed", hostsToRemove);
        return hostsToRemove;
    }

    public void decommissionAmbariNodes(Stack stack, Map<String, HostMetadata> hostsToRemove) throws IOException, URISyntaxException {
        LOGGER.info("Decommission ambari nodes: {}", hostsToRemove);
        AmbariClient ambariClient = getAmbariClient(stack);
        Map<String, HostStatus> hostStatusMap = ambariClientRetryer.getHostsStatuses(ambariClient, new ArrayList<>(hostsToRemove.keySet()));
        LOGGER.info("Host status map for decommission: {}", hostStatusMap);
        Map<String, HostMetadata> unhealthyHosts = new HashMap<>();
        Map<String, HostMetadata> healthyHosts = new HashMap<>();
        for (Entry<String, HostMetadata> hostToRemove : hostsToRemove.entrySet()) {
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
            for (Entry<String, HostMetadata> host : unhealthyHosts.entrySet()) {
                deleteHostFromAmbariIfInUnknownState(host.getValue(), hostStatusMap, ambariClient);
                hostMetadataRepository.delete(host.getValue());
                deletedHosts.add(host.getKey());
            }
        }

        LOGGER.info("Deleted hosts, after unhealthy hosts removal process: {}", deletedHosts);

        if (!healthyHosts.isEmpty()) {
            LOGGER.info("Healthy hosts are not empty, delete them also");
            deletedHosts.addAll(decommissionAmbariNodes(stack, healthyHosts, hostStatusMap, ambariClient));
        }

        LOGGER.info("Deleted hosts are: {}", deletedHosts);
    }

    private AmbariClient getAmbariClient(Stack stack) {
        Cluster cluster = stack.getCluster();
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp());
        return ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), cluster);
    }

    public Map<String, HostMetadata> collectHostsToRemove(Stack stack, String hostGroupName, Set<String> hostNames) {
        LOGGER.info("Collect hosts from '{}' to remove: '{}'", hostGroupName, hostNames);
        Map<String, HostMetadata> hostsToRemove = collectHostMetadata(stack.getCluster(), hostGroupName, hostNames);
        if (hostsToRemove.size() != hostNames.size()) {
            LOGGER.warn("Not all the hosts found in the given host group.");
        }
        AmbariClient ambariClient = getAmbariClient(stack);
        List<String> runningHosts = ambariClientRetryer.getClusterHosts(ambariClient);
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
    }

    public boolean deleteHostFromAmbariIfInUnknownState(Stack stack, HostMetadata data) throws IOException, URISyntaxException {
        LOGGER.info("Delete host from ambari {}", data);
        AmbariClient ambariClient = getAmbariClient(stack);
        Map<String, HostStatus> hostStatusMap = ambariClientRetryer.getHostsStatuses(ambariClient, List.of(data.getHostName()));
        return deleteHostFromAmbariIfInUnknownState(data, hostStatusMap, ambariClient);
    }

    public Map<String, Long> selectNodes(Map<String, Long> sortedAscending, Collection<HostMetadata> filteredHostList, int removeCount) {
        LOGGER.info("sortedAscending: {}, filteredHostList: {}", sortedAscending, filteredHostList);

        Map<String, Long> select = filteredHostList
                .stream()
                .filter(hostMetadata -> hostMetadata.getHostMetadataState() == HostMetadataState.UNHEALTHY)
                .limit(removeCount)
                .collect(toMap(HostMetadata::getHostName, o -> 0L));

        LOGGER.info("Selected UNHEALTHY nodes: {}", select);

        if (select.size() < removeCount) {
            LOGGER.info("Selected nodes size smaller than remove count, add some nodes");
            Set<String> hostNames = filteredHostList.stream().map(a -> a.getHostName().toLowerCase()).collect(Collectors.toSet());
            sortedAscending.entrySet().stream()
                    .filter(entry -> !select.keySet().contains(entry.getKey()) && hostNames.contains(entry.getKey().toLowerCase()))
                    .limit(removeCount - select.size())
                    .forEach(entry -> select.put(entry.getKey(), entry.getValue()));
        }
        LOGGER.info("Selected nodes to remove: {}", removeCount);
        return select;
    }

    private boolean deleteHostFromAmbariIfInUnknownState(HostMetadata data, Map<String, HostStatus> hostsStatuses, AmbariClient ambariClient)
            throws IOException, URISyntaxException {
        LOGGER.info("Delete host from ambari: {}", data);
        boolean hostDeleted = false;
        if (hostsStatuses.containsKey(data.getHostName())) {
            HostStatus hostStatus = hostsStatuses.get(data.getHostName());
            String hostState = hostStatus.getHostStatus();
            if ("UNKNOWN".equals(hostState)) {
                LOGGER.info("Host is in UNKNOWN state, lets delete: {}", data);
                ambariDeleteHostsService.deleteHostsButFirstQueryThemFromAmbari(ambariClient, singletonList(data.getHostName()));
                hostDeleted = true;
            }
        } else {
            LOGGER.info("Host is already deleted. {}", data);
            hostDeleted = true;
        }
        return hostDeleted;
    }

    private Collection<String> decommissionAmbariNodes(Stack stack, Map<String, HostMetadata> hostsToRemove, Map<String, HostStatus> hostStatusMap,
            AmbariClient ambariClient) {
        Collection<String> result = new HashSet<>();
        PollingResult pollingResult = startServicesIfNeeded(stack, ambariClient, hostStatusMap);
        try {
            if (isSuccess(pollingResult)) {
                LOGGER.info("All services started, let's decommission");
                List<String> hostList = new ArrayList<>(hostsToRemove.keySet());
                retireNifiNodes(stack, ambariClient, hostList, hostStatusMap);
                Map<String, Integer> decommissionRequests = decommissionComponents(ambariClient, hostList, hostStatusMap);
                if (!decommissionRequests.isEmpty()) {
                    LOGGER.info("Wait for Ambari decommission progress, decommission requests: ({})", decommissionRequests);
                    pollingResult =
                            ambariOperationService.waitForOperations(stack, ambariClient, decommissionRequests, DECOMMISSION_AMBARI_PROGRESS_STATE).getLeft();
                }
                if (!isSuccess(pollingResult)) {
                    LOGGER.warn("Polling failed, '{}' could not be decommissioned, polling result: '{}'", NODEMANAGER, pollingResult);
                    Map<String, HostStatus> statuses = ambariClientRetryer.getHostComponentStatuses(ambariClient, hostList, List.of(NODEMANAGER));
                    throw new DecommissionException("Nodemanager could not be decommissioned on hosts, current states " + statuses);
                }
                pollingResult = waitForDataNodeDecommission(stack, ambariClient, hostList, hostStatusMap);
                if (!isSuccess(pollingResult)) {
                    LOGGER.warn("Polling failed, '{}' could not be decommissioned, polling result: '{}'", DATANODE, pollingResult);
                    Map<String, HostStatus> statuses = ambariClientRetryer.getHostComponentStatuses(ambariClient, hostList, List.of(DATANODE));
                    throw new DecommissionException("Datanode could not be decommissioned on hosts, current states " + statuses);
                }
                pollingResult = waitForRegionServerDecommission(stack, ambariClient, hostList, hostStatusMap);
                if (!isSuccess(pollingResult)) {
                    LOGGER.warn("Polling failed, '{}' could not be decommissioned, polling result: '{}'", HBASE_REGIONSERVER, pollingResult);
                    Map<String, HostStatus> statuses = ambariClientRetryer.getHostComponentStatuses(ambariClient, hostList, List.of(HBASE_REGIONSERVER));
                    throw new DecommissionException("HBASE region server could not be decommissioned on hosts, current states " + statuses);
                }
                pollingResult = stopHadoopComponents(stack, ambariClient, hostList, hostStatusMap);
                if (!isSuccess(pollingResult)) {
                    LOGGER.warn("Polling failed, services could not be stopped on the cluster.");
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
        LOGGER.info("Decommissioned hosts: {}", result);
        return result;
    }

    private List<HostMetadata> collectDownscaleCandidatesFromCluster(Stack stack, Cluster cluster, String hostGroupName, Integer scalingAdjustment,
            boolean forced) {
        LOGGER.info("Collect downscale candidates from cluster. hostgroup: {}, scalingAdjustment: {}, forced: {}", hostGroupName, scalingAdjustment, forced);
        List<HostMetadata> downScaleCandidates;
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp());
        HostGroup hostGroup = hostGroupService.getByClusterIdAndNameWithHostMetadata(cluster.getId(), hostGroupName);
        Set<HostMetadata> hostsInHostGroup = hostGroup.getHostMetadata();
        LOGGER.info("Hosts in {} hostgroup: {}", hostGroup.getName(), hostsInHostGroup);
        List<HostMetadata> filteredHostList = hostFilterService.filterHostsForDecommission(cluster, hostsInHostGroup, hostGroupName);
        int reservedInstances = hostsInHostGroup.size() - filteredHostList.size();
        LOGGER.info("Reserved instances size: {}", reservedInstances);
        String blueprintName = cluster.getBlueprint().getAmbariName();
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), cluster);
        Map<String, List<String>> blueprintMap = ambariClientRetryer.getBlueprintMap(ambariClient, blueprintName);
        LOGGER.info("Blueprint map: {}", blueprintMap);
        if (hostGroupNodesAreDataNodes(blueprintMap, hostGroupName)) {
            LOGGER.info("Hostgroup nodes are datanodes");
            int replication = getReplicationFactor(ambariClient, hostGroupName);
            LOGGER.info("Replication is: {}", replication);
            if (!forced) {
                LOGGER.info("It isn't forced downscale, so verify node count");
                verifyNodeCount(replication, scalingAdjustment, filteredHostList.size(), reservedInstances);
            }
            downScaleCandidates = checkAndSortByAvailableSpace(stack, ambariClient, replication, scalingAdjustment, filteredHostList, forced);
        } else {
            LOGGER.info("Hostgroup nodes are not datanodes, but verify node count");
            verifyNodeCount(NO_REPLICATION, scalingAdjustment, filteredHostList.size(), reservedInstances);
            downScaleCandidates = filteredHostList;
        }
        LOGGER.info("Downscale candidates are: {}", downScaleCandidates);
        return downScaleCandidates;
    }

    private boolean hostGroupNodesAreDataNodes(Map<String, List<String>> blueprintMap, String hostGroupName) {
        return blueprintMap.get(hostGroupName).contains(DATANODE);
    }

    public void verifyNodesAreRemovable(Stack stack, List<InstanceMetaData> instanceMetaDataList) {
        LOGGER.info("Verify nodes are removable {}", instanceMetaDataList);
        AmbariClient ambariClient = getAmbariClient(stack);
        Cluster cluster = stack.getCluster();
        String blueprintName = cluster.getBlueprint().getAmbariName();
        Map<String, List<String>> blueprintMap = ambariClientRetryer.getBlueprintMap(ambariClient, blueprintName);
        LOGGER.info("Blueprint map for verifying: {}", blueprintMap);

        List<InstanceMetaData> instancesWithHostName = instanceMetaDataList.stream()
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                .collect(Collectors.toList());

        LOGGER.info("Instances with hostname: {}", instancesWithHostName);

        Multimap<Long, HostMetadata> hostGroupWithInstances = ArrayListMultimap.create();
        for (InstanceMetaData instanceMetaData : instancesWithHostName) {
            HostMetadata hostMetadata = hostGroupService.getHostMetadataByClusterAndHostName(stack.getCluster(), instanceMetaData.getDiscoveryFQDN());
            if (hostMetadata != null) {
                hostGroupWithInstances.put(hostMetadata.getHostGroup().getId(), hostMetadata);
            }
        }

        LOGGER.info("Collected hostgroups with instances {}", hostGroupWithInstances);

        try {
            transactionService.required(() -> {
                Set<HostGroup> hostGroups = hostGroupService.getByCluster(cluster.getId());
                for (HostGroup hostGroup : hostGroups) {
                    Collection<HostMetadata> removableHostsInHostGroup = hostGroupWithInstances.get(hostGroup.getId());
                    LOGGER.info("Removable hosts in hostgroup: {}", removableHostsInHostGroup);
                    if (removableHostsInHostGroup != null && !removableHostsInHostGroup.isEmpty()) {
                        String hostGroupName = hostGroup.getName();
                        List<HostMetadata> hostListForDecommission =
                                hostFilterService.filterHostsForDecommission(cluster, removableHostsInHostGroup, hostGroupName);
                        LOGGER.info("Host list for decommission: {}", hostListForDecommission);
                        boolean hostGroupContainsDatanode = hostGroupNodesAreDataNodes(blueprintMap, hostGroupName);
                        int replication = hostGroupContainsDatanode ? getReplicationFactor(ambariClient, hostGroupName) : NO_REPLICATION;
                        LOGGER.info("Replication is {}", replication);
                        if (hostListForDecommission.size() < removableHostsInHostGroup.size()) {
                            LOGGER.info("Removable hosts size is bigger than host list for decommission");
                            List<HostMetadata> notRecommendedRemovableNodes = removableHostsInHostGroup.stream()
                                    .filter(hostMetadata -> !hostListForDecommission.contains(hostMetadata))
                                    .collect(Collectors.toList());
                            throw new NotRecommendedNodeRemovalException("Following nodes shouldn't be removed from the cluster: "
                                    + notRecommendedRemovableNodes.stream().map(HostMetadata::getHostName).collect(Collectors.toList()));
                        }
                        verifyNodeCount(replication, removableHostsInHostGroup.size(), hostGroup.getHostMetadata().size(), 0);
                        if (hostGroupContainsDatanode) {
                            calculateDecommissioningTime(stack, hostListForDecommission, ambariClient);
                        }
                    }
                }
                return null;
            });
        } catch (TransactionExecutionException e) {
            throw e.getCause();
        }
    }

    private Map<String, HostMetadata> collectHostMetadata(Cluster cluster, String hostGroupName, Collection<String> hostNames) {
        LOGGER.info("Collect host metadata, hostgroup: {}, hostnames: {}", hostGroupName, hostNames);
        HostGroup hostGroup = hostGroupService.getByClusterIdAndNameWithHostMetadata(cluster.getId(), hostGroupName);
        Set<HostMetadata> hostsInHostGroup = hostGroup.getHostMetadata();
        return hostsInHostGroup.stream().filter(hostMetadata -> hostNames.contains(hostMetadata.getHostName())).collect(
                toMap(HostMetadata::getHostName, hostMetadata -> hostMetadata));
    }

    private int getReplicationFactor(AmbariClient ambariClient, String hostGroup) {
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

    private List<HostMetadata> checkAndSortByAvailableSpace(Stack stack, AmbariClient client, int replication, int adjustment,
            List<HostMetadata> filteredHostList, boolean forced) {
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
        if (remainingSpace < safetyUsedSpace && !forced) {
            throw new BadRequestException(
                    String.format("Trying to move '%s' bytes worth of data to nodes with '%s' bytes of capacity is not allowed", usedSpace, remainingSpace)
            );
        }

        ambariDecommissionTimeCalculator.calculateDecommissioningTime(stack, filteredHostList, dfsSpace, usedSpace);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_SELECT_FOR_DOWNSCALE, AVAILABLE.name(), selectedNodes.keySet());
        return convert(selectedNodes, filteredHostList);
    }

    private Map<String, Map<Long, Long>> getDFSSpace(Stack stack, AmbariClient client) {
        LOGGER.info("Get dfs space for stack: {}", stack.getName());
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
        LOGGER.info("Remove hosts from orchestrator: {}", hostNames);
        Orchestrator orchestrator = stack.getOrchestrator();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
        try {
            LOGGER.info("Orchestrator type: {}", orchestratorType);
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
            LOGGER.error("Failed to delete orchestrator components while decommissioning: ", e);
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
            Map<String, HostStatus> hostStatusMap) {
        LOGGER.info("Waiting for '{}' decommission on hosts: {}", DATANODE, hosts);
        if (hosts.stream().noneMatch(hn -> hostStatusMap.get(hn).getHostComponentsStatuses().keySet().contains(DATANODE))) {
            return SUCCESS;
        }

        LOGGER.info("Waiting for DataNodes to move the blocks to other nodes. stack id: {}", stack.getId());
        return ambariOperationService.waitForOperations(stack, ambariClient, dnDecommissionStatusCheckerTask, Collections.emptyMap(),
                DECOMMISSION_SERVICES_AMBARI_PROGRESS_STATE).getLeft();
    }

    private PollingResult waitForRegionServerDecommission(Stack stack, AmbariClient ambariClient, List<String> hosts,
            Map<String, HostStatus> hostStatusMap) {
        LOGGER.info("Waiting for '{}' decommission on hosts: {}", HBASE_REGIONSERVER, hosts);
        if (COMPONENTS_NEED_TO_DECOMMISSION.get(HBASE_REGIONSERVER) == null
                || hosts.stream().noneMatch(hn -> hostStatusMap.get(hn).getHostComponentsStatuses().keySet().contains(HBASE_REGIONSERVER))) {
            return SUCCESS;
        }

        LOGGER.info("Waiting for RegionServers to move the regions to other servers");
        return rsPollerService.pollWithTimeoutSingleFailure(rsDecommissionStatusCheckerTask, new AmbariHostsWithNames(stack, ambariClient, hosts),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_REGION_DECOM);
    }

    private Set<String> selectHostsToRemove(List<HostMetadata> decommissionCandidates, int adjustment) {
        LOGGER.info("Adjustment is {}, select hosts to remove from decommission candidates: {}", adjustment, decommissionCandidates);
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
        LOGGER.info("Hosts '{}' will be removed from Ambari cluster", hostsToRemove);
        return hostsToRemove;
    }

    private void retireNifiNodes(Stack stack, AmbariClient ambariClient, List<String> hostList, Map<String, HostStatus> hostStatusMap) {
        List<String> hostsRunService = hostList.stream().filter(hn -> hostStatusMap.get(hn).getHostComponentsStatuses().containsKey(NIFI))
                .collect(Collectors.toList());
        hostsRunService.stream().forEach(nifiNode -> {
            LOGGER.info("Retiring NiFi node {}", nifiNode);
            Pair<PollingResult, Exception> retireResult = Pair.of(SUCCESS, null);
            try {
                int requestId = ambariClient.retire(List.of(nifiNode), "NIFI", NIFI);
                retireResult = ambariOperationService.waitForOperations(stack, ambariClient,
                        singletonMap("Retiring NiFi node", requestId), RETIRE_NIFI_NODE_AMBARI_PROGRESS_STATE);
            } catch (Exception e) {
                handleRetireError(nifiNode, e);
            }
            if (isSuccess(retireResult.getKey())) {
                LOGGER.info("Retirement of NiFi node {} was successful.", nifiNode);
            } else {
                handleRetireError(nifiNode, retireResult.getValue());
            }
        });
    }

    private void handleRetireError(String node, Exception exception) {
        LOGGER.error("Retirement of NiFi node {} failed: {}", node, exception.getMessage());
        throw new DecommissionException(exception);
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
                            return ambariClientRetryer.decommissionNodeManagers(ambariClient, h);
                        } catch (URISyntaxException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    };
                    break;
                case DATANODE:
                    action = h -> {
                        try {
                            return ambariClientRetryer.decommissionDataNodes(ambariClient, h);
                        } catch (URISyntaxException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    };
                    break;
                case HBASE_REGIONSERVER:
                    action = l -> {
                        try {
                            ambariClientRetryer.setHBaseRegionServersToMaintenance(ambariClient, l, true);
                            return ambariClientRetryer.decommissionHBaseRegionServers(ambariClient, l);
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
        LOGGER.info("Stopping services on hosts: {}", hosts);
        try {
            hosts = hosts.stream()
                    .filter(hn -> !hostStatusMap.get(hn).getHostComponentsStatuses().isEmpty()).collect(Collectors.toList());
            if (!hosts.isEmpty()) {
                int requestId = ambariClientRetryer.stopAllComponentsOnHosts(ambariClient, hosts);
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
                int requestId = ambariClientRetryer.startService(ambariClient, service);
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

    private void calculateDecommissioningTime(Stack stack, Collection<HostMetadata> filteredHostList, AmbariClient ambariClient) {
        LOGGER.info("Calculate decomissioning time for hosts: {}", filteredHostList);
        Map<String, Map<Long, Long>> dfsSpace = getDFSSpace(stack, ambariClient);
        Map<String, Long> sortedAscending = sortByUsedSpace(dfsSpace, false);
        Map<String, Long> usedSpaceByHostname = sortedAscending.entrySet().stream()
                .filter(entry -> filteredHostList.stream().anyMatch(hm -> hm.getHostName().equalsIgnoreCase(entry.getKey())))
                .collect(toMap(Entry::getKey, Entry::getValue));
        long usedSpace = getSelectedUsage(usedSpaceByHostname);
        ambariDecommissionTimeCalculator.calculateDecommissioningTime(stack, filteredHostList, dfsSpace, usedSpace);
    }
}
