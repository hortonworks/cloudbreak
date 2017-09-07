package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_AGENT;
import static com.sequenceiq.cloudbreak.service.PollingResult.SUCCESS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.service.PollingResult.isTimeout;
import static com.sequenceiq.cloudbreak.service.cluster.DataNodeUtils.sortByUsedSpace;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.AMBARI_POLLING_INTERVAL;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.MAX_ATTEMPTS_FOR_HOSTS;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.DECOMMISSION_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.DECOMMISSION_SERVICES_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.START_SERVICES_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.STOP_SERVICES_AMBARI_PROGRESS_STATE;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import java.util.ArrayList;
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

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.repository.ContainerRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.AmbariConfigurationService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.service.cluster.ConfigParam;
import com.sequenceiq.cloudbreak.service.cluster.filter.HostFilterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.util.AmbariClientExceptionUtil;

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

    public Set<String> decommissionAmbariNodes(Stack stack, String hostGroupName, Set<String> hostNames) throws CloudbreakException {
        Map<String, HostMetadata> hostsToRemove = collectHostMetadata(stack.getCluster(), hostGroupName, hostNames);
        if (hostsToRemove.size() != hostNames.size()) {
            throw new CloudbreakException("Not all the hosts found in the given host group.");
        }
        Cluster cluster = stack.getCluster();
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), cluster);
        List<String> runningHosts = ambariClient.getClusterHosts();
        new HashSet(hostsToRemove.keySet()).forEach(hostName -> {
            if (!runningHosts.contains(hostName)) {
                hostsToRemove.remove(hostName);
            }
        });
        if (hostsToRemove.isEmpty()) {
            return hostNames;
        }
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
            List<String> hostList = new ArrayList<>(hostsToRemove.keySet());
            removeHostsFromOrchestrator(stack, ambariClient, hostList);
            for (Entry<String, HostMetadata> host : unhealthyHosts.entrySet()) {
                deleteHostFromAmbari(host.getValue(), runningComponents, ambariClient);
                hostMetadataRepository.delete(host.getValue().getId());
                deletedHosts.add(host.getKey());
            }
        }
        if (!healthyHosts.isEmpty()) {
            deletedHosts.addAll(decommissionAmbariNodes(stack, healthyHosts, runningComponents, ambariClient));
        }
        return deletedHosts;
    }

    public boolean deleteHostFromAmbari(Stack stack, HostMetadata data) throws CloudbreakSecuritySetupException {
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), stack.getCluster().getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), stack.getCluster());
        Map<String, Map<String, String>> runningComponents = ambariClient.getHostComponentsStates();
        return deleteHostFromAmbari(data, runningComponents, ambariClient);
    }

    private boolean deleteHostFromAmbari(HostMetadata data, Map<String, Map<String, String>> runningComponents, AmbariClient ambariClient) {
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

    private Set<String> decommissionAmbariNodes(Stack stack, Map<String, HostMetadata> hostsToRemove, Map<String, Map<String, String>> runningComponents,
            AmbariClient ambariClient) throws CloudbreakException {
        Set<String> result = new HashSet<>();
        PollingResult pollingResult = startServicesIfNeeded(stack, ambariClient, runningComponents);
        if (isSuccess(pollingResult)) {
            List<String> hostList = new ArrayList<>(hostsToRemove.keySet());
            Map<String, Integer> decommissionRequests = decommissionComponents(ambariClient, hostList, runningComponents);
            if (!decommissionRequests.isEmpty()) {
                pollingResult = ambariOperationService.waitForOperations(stack, ambariClient, decommissionRequests, DECOMMISSION_AMBARI_PROGRESS_STATE);
            }
            if (isSuccess(pollingResult)) {
                pollingResult = waitForDataNodeDecommission(stack, ambariClient, hostList, runningComponents);
                if (isSuccess(pollingResult)) {
                    pollingResult = waitForRegionServerDecommission(stack, ambariClient, hostList, runningComponents);
                    if (isSuccess(pollingResult)) {
                        pollingResult = stopHadoopComponents(stack, ambariClient, hostList, runningComponents);
                        if (isSuccess(pollingResult)) {
                            pollingResult = removeHostsFromOrchestrator(stack, ambariClient, hostList);
                            if (isSuccess(pollingResult) || isTimeout(pollingResult)) {
                                deleteHosts(hostList, runningComponents, ambariClient);
                                result.addAll(hostsToRemove.keySet());
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private void deleteHosts(List<String> hosts, Map<String, Map<String, String>> components, AmbariClient client) {
        for (String hostName : hosts) {
            client.deleteHostComponents(hostName, new ArrayList<>(components.get(hostName).keySet()));
            client.deleteHost(hostName);
        }
    }

    private List<HostMetadata> collectDownscaleCandidates(Stack stack, Cluster cluster, String hostGroupName, Integer scalingAdjustment)
            throws CloudbreakSecuritySetupException {
        List<HostMetadata> downScaleCandidates;
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp());
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(cluster.getId(), hostGroupName);
        Set<HostMetadata> hostsInHostGroup = hostGroup.getHostMetadata();
        List<HostMetadata> filteredHostList = hostFilterService.filterHostsForDecommission(cluster, hostsInHostGroup, hostGroupName);
        int reservedInstances = hostsInHostGroup.size() - filteredHostList.size();
        String blueprintName = cluster.getBlueprint().getBlueprintName();
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), cluster);
        if (ambariClient.getBlueprintMap(blueprintName).get(hostGroupName).contains(DATANODE)) {
            int replication = getReplicationFactor(ambariClient, hostGroupName);
            verifyNodeCount(replication, scalingAdjustment, filteredHostList, reservedInstances);
            downScaleCandidates = checkAndSortByAvailableSpace(stack, ambariClient, replication, scalingAdjustment, filteredHostList);
        } else {
            verifyNodeCount(NO_REPLICATION, scalingAdjustment, filteredHostList, reservedInstances);
            downScaleCandidates = filteredHostList;
        }
        return downScaleCandidates;
    }

    private Map<String, HostMetadata> collectHostMetadata(Cluster cluster, String hostGroupName, Set<String> hostNames) {
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(cluster.getId(), hostGroupName);
        Set<HostMetadata> hostsInHostGroup = hostGroup.getHostMetadata();
        Map<String, HostMetadata> hostMetadatas = hostsInHostGroup.stream().filter(hostMetadata -> hostNames.contains(hostMetadata.getHostName())).collect(
                Collectors.toMap(hostMetadata -> hostMetadata.getHostName(), hostMetadata -> hostMetadata));
        return hostMetadatas;
    }

    private int getReplicationFactor(AmbariClient ambariClient, String hostGroup) {
        Map<String, String> configuration = configurationService.getConfiguration(ambariClient, hostGroup);
        return Integer.parseInt(configuration.get(ConfigParam.DFS_REPLICATION.key()));
    }

    private void verifyNodeCount(int replication, int scalingAdjustment, List<HostMetadata> filteredHostList, int reservedInstances) {
        int adjustment = Math.abs(scalingAdjustment);
        int hostSize = filteredHostList.size();
        if (hostSize + reservedInstances - adjustment < replication || hostSize < adjustment) {
            LOGGER.info("Cannot downscale: replication: {}, adjustment: {}, filtered host size: {}", replication, scalingAdjustment, hostSize);
            throw new BadRequestException("There is not enough node to downscale. "
                    + "Check the replication factor and the ApplicationMaster occupation.");
        }
    }

    private List<HostMetadata> checkAndSortByAvailableSpace(Stack stack, AmbariClient client, int replication, int adjustment,
            List<HostMetadata> filteredHostList) {
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

    private Map<String, Long> selectNodes(Map<String, Long> sortedAscending, List<HostMetadata> filteredHostList, int removeCount) {
        LOGGER.info("sortedAscending: {}, filteredHostList: {}", sortedAscending, filteredHostList);
        Map<String, Long> select = new HashMap<>();
        int i = 0;
        for (Entry<String, Long> entry : sortedAscending.entrySet()) {
            if (i < removeCount) {
                for (HostMetadata hostMetadata : filteredHostList) {
                    if (hostMetadata.getHostName().equalsIgnoreCase(entry.getKey())) {
                        select.put(entry.getKey(), entry.getValue());
                        i++;
                        break;
                    }
                }
            } else {
                break;
            }
        }
        return select;
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

    private List<HostMetadata> convert(Map<String, Long> selectedNodes, List<HostMetadata> filteredHostList) {
        List<HostMetadata> result = new ArrayList<>();
        for (String host : selectedNodes.keySet()) {
            for (HostMetadata hostMetadata : filteredHostList) {
                if (hostMetadata.getHostName().equalsIgnoreCase(host)) {
                    result.add(hostMetadata);
                    break;
                }
            }
        }
        return result;
    }

    private PollingResult removeHostsFromOrchestrator(Stack stack, AmbariClient ambariClient, List<String> hostNames) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        Map<String, Object> map = new HashMap<>();
        map.putAll(orchestrator.getAttributes().getMap());
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
        try {
            if (orchestratorType.containerOrchestrator()) {
                OrchestrationCredential credential = new OrchestrationCredential(orchestrator.getApiEndpoint(), map);
                ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get(orchestrator.getType());
                Set<Container> containers = containerRepository.findContainersInCluster(stack.getCluster().getId());
                List<ContainerInfo> containersToDelete = containers.stream()
                        .filter(input -> hostNames.contains(input.getHost()) && input.getImage().contains(AMBARI_AGENT.getName()))
                        .map(input -> new ContainerInfo(input.getContainerId(), input.getName(), input.getHost(), input.getImage()))
                        .collect(Collectors.toList());
                containerOrchestrator.deleteContainer(containersToDelete, credential);
                containerRepository.delete(containers);
                return waitForHostsToLeave(stack, ambariClient, hostNames);
            } else if (orchestratorType.hostOrchestrator()) {
                HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
                Map<String, String> privateIpsByFQDN = new HashMap<>();
                stack.getInstanceMetaDataAsList().stream()
                        .filter(instanceMetaData -> hostNames.stream().anyMatch(hn -> hn.contains(instanceMetaData.getDiscoveryFQDN().split("\\.")[0])))
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

    private PollingResult waitForHostsToLeave(Stack stack, AmbariClient ambariClient, List<String> hostNames) {
        return ambariHostLeave.pollWithTimeout(hostsLeaveStatusCheckerTask, new AmbariHostsWithNames(stack, ambariClient, hostNames),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_HOSTS, AmbariOperationService.MAX_FAILURE_COUNT);
    }

    private PollingResult waitForDataNodeDecommission(Stack stack, AmbariClient ambariClient, List<String> hosts,
            Map<String, Map<String, String>> runningComponents) {
        if (hosts.stream().noneMatch(hn -> runningComponents.get(hn).keySet().contains(DATANODE))) {
            return SUCCESS;
        }

        LOGGER.info("Waiting for DataNodes to move the blocks to other nodes. stack id: {}", stack.getId());
        return ambariOperationService.waitForOperations(stack, ambariClient, dnDecommissionStatusCheckerTask, Collections.emptyMap(),
                DECOMMISSION_SERVICES_AMBARI_PROGRESS_STATE);
    }

    private PollingResult waitForRegionServerDecommission(Stack stack, AmbariClient ambariClient, List<String> hosts,
            Map<String, Map<String, String>> runningComponents) {
        if (COMPONENTS_NEED_TO_DECOMMISSION.get("HBASE_REGIONSERVER") == null
                || hosts.stream().noneMatch(hn -> runningComponents.get(hn).keySet().contains("HBASE_REGIONSERVER"))) {
            return SUCCESS;
        }

        LOGGER.info("Waiting for RegionServers to move the regions to other servers");
        return rsPollerService.pollWithTimeoutSingleFailure(rsDecommissionStatusCheckerTask, new AmbariHostsWithNames(stack, ambariClient, hosts),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_REGION_DECOM);
    }

    private Set<String> selectHostsToRemove(List<HostMetadata> decommissionCandidates, int adjustment) {
        Set<String> hostsToRemove = new HashSet<>();
        int i = 0;
        for (HostMetadata hostMetadata : decommissionCandidates) {
            String hostName = hostMetadata.getHostName();
            if (i < adjustment) {
                LOGGER.info("Host '{}' will be removed from Ambari cluster", hostName);
                hostsToRemove.add(hostName);
            } else {
                break;
            }
            i++;
        }
        return hostsToRemove;
    }

    private Map<String, Integer> decommissionComponents(AmbariClient ambariClient, List<String> hosts, Map<String, Map<String, String>> runningComponents) {
        Map<String, Integer> decommissionRequests = new HashMap<>();
        COMPONENTS_NEED_TO_DECOMMISSION.keySet().forEach(component -> {
            List<String> hostsRunService = hosts.stream().filter(hn -> runningComponents.get(hn).keySet().contains(component)).collect(Collectors.toList());
            Function<List<String>, Integer> action;
            if ("NODEMANAGER".equals(component)) {
                action = l -> ambariClient.decommissionNodeManagers(l);
            } else if (component.equals(DATANODE)) {
                action = l -> ambariClient.decommissionDataNodes(l);
            } else if ("HBASE_REGIONSERVER".equals(component)) {
                action = l -> {
                    ambariClient.setHBaseRegionServersToMaintenance(l, true);
                    return ambariClient.decommissionHBaseRegionServers(l);
                };
            } else {
                throw new UnsupportedOperationException("Component decommission not allowed: " + component);
            }
            Integer requestId = decommissionComponent(ambariClient, hostsRunService, component, action);
            if (requestId != null) {
                decommissionRequests.put(component + "_DECOMMISSION", requestId);
            }
        });
        return decommissionRequests;
    }

    private Integer decommissionComponent(AmbariClient ambariClient, List<String> hosts, String component, Function<List<String>, Integer> action) {
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
                        STOP_SERVICES_AMBARI_PROGRESS_STATE);
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
                requests.put(service + "_START", requestId);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Failed to start HDFS/YARN/HBASE services", e);
            throw new BadRequestException("Failed to start the HDFS, YARN and HBASE services, it's possible that some of the nodes are unavailable");
        }

        if (!requests.isEmpty()) {
            return ambariOperationService.waitForOperations(stack, ambariClient, requests, START_SERVICES_AMBARI_PROGRESS_STATE);
        } else {
            return SUCCESS;
        }
    }

    private Set<String> collectServicesToStart(AmbariClient ambariClient, Map<String, Map<String, String>> runningComponents) {
        Set<String> services = new HashSet<>();
        for (Entry<String, Map<String, String>> hostComponentsEntry : runningComponents.entrySet()) {
            for (Entry<String, String> componentStateEntry : hostComponentsEntry.getValue().entrySet()) {
                String component = componentStateEntry.getKey();
                if (!"STARTED".equals(componentStateEntry.getValue()) && COMPONENTS_NEED_TO_DECOMMISSION.keySet().contains(component)) {
                    Map<String, String> componentStates = ambariClient.getComponentStates(hostComponentsEntry.getKey(), component);
                    if ("DECOMMISSIONED".equals(componentStates.get("desired_admin_state"))) {
                        LOGGER.info("No need to start ambari service {} on host {}", component, hostComponentsEntry.getKey());
                    } else {
                        services.add(COMPONENTS_NEED_TO_DECOMMISSION.get(component));
                    }
                }
            }
        }
        return services;
    }
}
