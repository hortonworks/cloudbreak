package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_AGENT;
import static com.sequenceiq.cloudbreak.service.PollingResult.SUCCESS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.service.PollingResult.isTimeout;
import static com.sequenceiq.cloudbreak.service.cluster.DataNodeUtils.sortByUsedSpace;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.AMBARI_POLLING_INTERVAL;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.MAX_ATTEMPTS_FOR_HOSTS;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.DECOMMISSION_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.DECOMMISSION_SERVICES_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.START_SERVICES_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.STOP_SERVICES_AMBARI_PROGRESS_STATE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.model.Status;
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
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.ContainerRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
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
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.util.AmbariClientExceptionUtil;

import groovyx.net.http.HttpResponseException;

@Component
public class AmbariDecommissioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariDecommissioner.class);

    private static final int MAX_ATTEMPTS_FOR_REGION_DECOM = 500;
    private static final String DATANODE = "DATANODE";
    private static final double SAFETY_PERCENTAGE = 1.2;
    private static final int NO_REPLICATION = 0;

    @Inject
    private StackRepository stackRepository;
    @Inject
    private ClusterRepository clusterRepository;
    @Inject
    private HostGroupService hostGroupService;
    @Inject
    private AmbariClientProvider ambariClientProvider;
    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;
    @Inject
    private CloudbreakEventService eventService;
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

    private enum Msg {
        AMBARI_CLUSTER_REMOVING_NODE_FROM_HOSTGROUP("ambari.cluster.removing.node.from.hostgroup");

        private String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }

    public Set<String> decommissionAmbariNodes(Stack stack, String hostGroupName, Integer scalingAdjustment) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        int adjustment = Math.abs(scalingAdjustment);
        Map<String, HostMetadata> hostsToRemove = selectHostsToRemove(collectDownscaleCandidates(stack, cluster, hostGroupName, adjustment), adjustment);
        if (hostsToRemove.size() != adjustment) {
            throw new CloudbreakException(String.format("Only %d hosts found to downscale but %d required.", hostsToRemove.size(), adjustment));
        }
        Set<String> result = new HashSet<>();
        LOGGER.info("Decommissioning {} hosts from host group '{}'", adjustment, hostGroupName);
        eventService.fireCloudbreakInstanceGroupEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_REMOVING_NODE_FROM_HOSTGROUP.code(), asList(adjustment, hostGroupName)), hostGroupName);
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getSecureAmbariClient(clientConfig, stack.getGatewayPort(), cluster);
        String blueprintName = stack.getCluster().getBlueprint().getBlueprintName();
        PollingResult pollingResult = startServicesIfNeeded(stack, ambariClient, blueprintName);
        if (isSuccess(pollingResult)) {
            Set<String> components = getHadoopComponents(ambariClient, hostGroupName, blueprintName);
            List<String> hostList = new ArrayList<>(hostsToRemove.keySet());
            pollingResult = ambariOperationService.waitForOperations(stack, ambariClient, decommissionComponents(ambariClient, hostList, components),
                    DECOMMISSION_AMBARI_PROGRESS_STATE);
            if (isSuccess(pollingResult)) {
                pollingResult = waitForDataNodeDecommission(stack, ambariClient);
                if (isSuccess(pollingResult)) {
                    //TODO https://hortonworks.jira.com/browse/BUG-53283
                    //TODO for now we only wait for the Ambari request to finish
                    //pollingResult = waitForRegionServerDecommission(stack, ambariClient, hostList, components);
                    if (isSuccess(pollingResult)) {
                        pollingResult = stopHadoopComponents(stack, ambariClient, hostList);
                        if (isSuccess(pollingResult)) {
                            stopAndDeleteHosts(stack, ambariClient, hostList, components);
                            cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
                            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(cluster.getId(), hostGroupName);
                            hostGroup.getHostMetadata().removeAll(hostsToRemove.values());
                            hostGroupService.save(hostGroup);
                            result.addAll(hostsToRemove.keySet());
                        }
                    }
                }
            }
        }
        return result;
    }

    public boolean deleteHostFromAmbari(Stack stack, HostMetadata data) throws CloudbreakSecuritySetupException {
        boolean hostDeleted = false;
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), stack.getCluster().getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getSecureAmbariClient(clientConfig, stack.getGatewayPort(), stack.getCluster());
        Set<String> components = getHadoopComponents(ambariClient, data.getHostGroup().getName(), stack.getCluster().getBlueprint().getBlueprintName());
        if (ambariClient.getClusterHosts().contains(data.getHostName())) {
            String hostState = ambariClient.getHostState(data.getHostName());
            if ("UNKNOWN".equals(hostState)) {
                deleteHosts(stack, singletonList(data.getHostName()), new ArrayList<>(components));
                hostDeleted = true;
            }
        } else {
            LOGGER.debug("Host is already deleted.");
            hostDeleted = true;
        }
        return hostDeleted;
    }

    private Set<String> getHadoopComponents(AmbariClient ambariClient, String hostGroupName, String blueprintName) {
        return ambariClient.getComponentsCategory(blueprintName, hostGroupName).keySet();
    }

    private void deleteHosts(Stack stack, List<String> hosts, List<String> components) throws CloudbreakSecuritySetupException {
        Cluster cluster = stack.getCluster();
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), cluster.getUserName(), cluster.getPassword());
        for (String hostName : hosts) {
            ambariClient.deleteHostComponents(hostName, components);
            ambariClient.deleteHost(hostName);
        }
    }

    private List<HostMetadata> collectDownscaleCandidates(Stack stack, Cluster cluster, String hostGroupName, Integer scalingAdjustment)
            throws CloudbreakSecuritySetupException {
        List<HostMetadata> downScaleCandidates;
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), cluster.getUserName(), cluster.getPassword());
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(cluster.getId(), hostGroupName);
        Set<HostMetadata> hostsInHostGroup = hostGroup.getHostMetadata();
        List<HostMetadata> filteredHostList = hostFilterService.filterHostsForDecommission(cluster, hostsInHostGroup, hostGroupName);
        int reservedInstances = hostsInHostGroup.size() - filteredHostList.size();
        String blueprintName = cluster.getBlueprint().getBlueprintName();
        if (doesHostGroupContainDataNode(ambariClient, blueprintName, hostGroupName)) {
            int replication = getReplicationFactor(ambariClient, hostGroupName);
            verifyNodeCount(replication, scalingAdjustment, filteredHostList, reservedInstances);
            downScaleCandidates = checkAndSortByAvailableSpace(stack, ambariClient, replication, scalingAdjustment, filteredHostList);
        } else {
            verifyNodeCount(NO_REPLICATION, scalingAdjustment, filteredHostList, reservedInstances);
            downScaleCandidates = filteredHostList;
        }
        return downScaleCandidates;
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

    private boolean doesHostGroupContainDataNode(AmbariClient client, String blueprint, String hostGroup) {
        return client.getBlueprintMap(blueprint).get(hostGroup).contains(DATANODE);
    }

    private List<HostMetadata> checkAndSortByAvailableSpace(Stack stack, AmbariClient client, int replication, int adjustment,
            List<HostMetadata> filteredHostList) {
        int removeCount = Math.abs(adjustment);
        Map<String, Map<Long, Long>> dfsSpace = getDFSSpace(stack, client);
        Map<String, Long> sortedAscending = sortByUsedSpace(dfsSpace, false);
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
        if (result == PollingResult.SUCCESS) {
            return dfsSpaceTask.getDfsSpace();
        } else {
            throw new CloudbreakServiceException("Failed to get dfs space from ambari!");

        }
    }

    private Map<String, Long> selectNodes(Map<String, Long> sortedAscending, List<HostMetadata> filteredHostList, int removeCount) {
        Map<String, Long> select = new HashMap<>();
        int i = 0;
        for (String host : sortedAscending.keySet()) {
            if (i < removeCount) {
                for (HostMetadata hostMetadata : filteredHostList) {
                    if (hostMetadata.getHostName().equalsIgnoreCase(host)) {
                        select.put(host, sortedAscending.get(host));
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
        for (String host : selected.keySet()) {
            usage += selected.get(host);
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

    private void stopAndDeleteHosts(Stack stack, AmbariClient ambariClient, final List<String> hostList, Set<String> components) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        Map<String, Object> map = new HashMap<>();
        map.putAll(orchestrator.getAttributes().getMap());
        map.put("certificateDir", tlsSecurityService.prepareCertDir(stack.getId()));
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
        try {
            if (orchestratorType.containerOrchestrator()) {
                OrchestrationCredential credential = new OrchestrationCredential(orchestrator.getApiEndpoint(), map);
                ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get(orchestrator.getType());
                Set<Container> containers = containerRepository.findContainersInCluster(stack.getCluster().getId());

                List<ContainerInfo> containersToDelete = containers.stream()
                        .filter(input -> hostList.contains(input.getHost()) && input.getImage().contains(AMBARI_AGENT.getName()))
                        .map(input -> new ContainerInfo(input.getContainerId(), input.getName(), input.getHost(), input.getImage()))
                        .collect(Collectors.toList());

                containerOrchestrator.deleteContainer(containersToDelete, credential);
                containerRepository.delete(containers);
                PollingResult pollingResult = waitForHostsToLeave(stack, ambariClient, hostList);
                if (isTimeout(pollingResult)) {
                    LOGGER.warn("Ambari agent stop timed out, delete the hosts anyway, hosts: {}", hostList);
                }
                if (!isExited(pollingResult)) {
                    deleteHosts(stack, hostList, components);
                }
            } else if (orchestratorType.hostOrchestrator()) {
                HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
                GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack);
                hostOrchestrator.tearDown(gatewayConfig, hostList);
                deleteHosts(stack, hostList, components);
            }
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.error("Failed to delete containers while decommissioning: ", e);
            throw new CloudbreakException("Failed to delete containers while decommissioning: ", e);
        }
    }

    private void deleteHosts(Stack stack, List<String> hostList, Set<String> components) throws CloudbreakSecuritySetupException {
        deleteHosts(stack, hostList, new ArrayList<>(components));
    }

    private PollingResult waitForHostsToLeave(Stack stack, AmbariClient ambariClient, List<String> hostNames) {
        return ambariHostLeave.pollWithTimeout(hostsLeaveStatusCheckerTask, new AmbariHostsWithNames(stack, ambariClient, hostNames),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_HOSTS, AmbariOperationService.MAX_FAILURE_COUNT);
    }

    private PollingResult waitForDataNodeDecommission(Stack stack, AmbariClient ambariClient) {
        LOGGER.info("Waiting for DataNodes to move the blocks to other nodes. stack id: {}", stack.getId());
        return ambariOperationService.waitForOperations(stack, ambariClient, dnDecommissionStatusCheckerTask, Collections.emptyMap(),
                DECOMMISSION_SERVICES_AMBARI_PROGRESS_STATE);
    }

    private PollingResult waitForRegionServerDecommission(Stack stack, AmbariClient ambariClient, List<String> hosts, Set<String> components) {
        if (!components.contains("HBASE_REGIONSERVER")) {
            return SUCCESS;
        }
        LOGGER.info("Waiting for RegionServers to move the regions to other servers");
        return rsPollerService.pollWithTimeoutSingleFailure(rsDecommissionStatusCheckerTask, new AmbariHostsWithNames(stack, ambariClient, hosts),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_REGION_DECOM);
    }

    private Map<String, HostMetadata> selectHostsToRemove(List<HostMetadata> decommissionCandidates, int adjustment) {
        Map<String, HostMetadata> hostsToRemove = new HashMap<>();
        int i = 0;
        for (HostMetadata hostMetadata : decommissionCandidates) {
            String hostName = hostMetadata.getHostName();
            if (i < adjustment) {
                LOGGER.info("Host '{}' will be removed from Ambari cluster", hostName);
                hostsToRemove.put(hostName, hostMetadata);
            } else {
                break;
            }
            i++;
        }
        return hostsToRemove;
    }

    private Map<String, Integer> decommissionComponents(AmbariClient ambariClient, List<String> hosts, Set<String> components) {
        Map<String, Integer> decommissionRequests = new HashMap<>();
        if (components.contains("NODEMANAGER")) {
            int requestId = ambariClient.decommissionNodeManagers(hosts);
            decommissionRequests.put("NODEMANAGER_DECOMMISSION", requestId);
        }
        if (components.contains("DATANODE")) {
            int requestId = ambariClient.decommissionDataNodes(hosts);
            decommissionRequests.put("DATANODE_DECOMMISSION", requestId);
        }
        if (components.contains("HBASE_REGIONSERVER")) {
            ambariClient.setHBaseRegionServersToMaintenance(hosts, true);
            int requestId = ambariClient.decommissionHBaseRegionServers(hosts);
            decommissionRequests.put("HBASE_DECOMMISSION", requestId);
        }
        return decommissionRequests;
    }

    private PollingResult stopHadoopComponents(Stack stack, AmbariClient ambariClient, List<String> hosts) {
        try {
            int requestId = ambariClient.stopAllComponentsOnHosts(hosts);
            return ambariOperationService.waitForOperations(stack, ambariClient, singletonMap("Stopping components on the decommissioned hosts", requestId),
                    STOP_SERVICES_AMBARI_PROGRESS_STATE);
        } catch (HttpResponseException e) {
            String errorMessage = AmbariClientExceptionUtil.getErrorMessage(e);
            throw new AmbariOperationFailedException("Ambari could not stop components. " + errorMessage, e);
        }
    }

    private PollingResult startServicesIfNeeded(Stack stack, AmbariClient ambariClient, String blueprint) {
        Map<String, Integer> stringIntegerMap = new HashMap<>();
        Map<String, String> componentsCategory = ambariClient.getComponentsCategory(blueprint);
        Map<String, Map<String, String>> hostComponentsStates = ambariClient.getHostComponentsStates();
        Set<String> services = new HashSet<>();
        collectServicesToStart(componentsCategory, hostComponentsStates, services);
        if (!services.isEmpty()) {
            try {
                if (services.contains("HDFS")) {
                    int requestId = ambariClient.startService("HDFS");
                    stringIntegerMap.put("HDFS_START", requestId);
                }
                if (services.contains("HBASE")) {
                    int requestId = ambariClient.startService("HBASE");
                    stringIntegerMap.put("HBASE_START", requestId);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to start HDFS/HBASE", e);
                throw new BadRequestException("Failed to start the HDFS and HBASE services, it's possible that some of the nodes are unavailable");
            }
        }

        if (!stringIntegerMap.isEmpty()) {
            return ambariOperationService.waitForOperations(stack, ambariClient, stringIntegerMap, START_SERVICES_AMBARI_PROGRESS_STATE);
        } else {
            return SUCCESS;
        }
    }

    private void collectServicesToStart(Map<String, String> componentsCategory, Map<String, Map<String, String>> hostComponentsStates, Set<String> services) {
        for (Map.Entry<String, Map<String, String>> hostComponentsEntry : hostComponentsStates.entrySet()) {
            Map<String, String> componentStateMap = hostComponentsEntry.getValue();
            for (Map.Entry<String, String> componentStateEntry : componentStateMap.entrySet()) {
                String componentKey = componentStateEntry.getKey();
                String category = componentsCategory.get(componentKey);
                if (!"CLIENT".equals(category)) {
                    if (!"STARTED".equals(componentStateEntry.getValue())) {
                        if ("NODEMANAGER".equals(componentKey) || "DATANODE".equals(componentKey)) {
                            services.add("HDFS");
                        } else if ("HBASE_REGIONSERVER".equals(componentKey)) {
                            services.add("HBASE");
                        } else {
                            LOGGER.info("No need to restart ambari service: {}", componentKey);
                        }
                    } else {
                        LOGGER.info("Ambari service already running: {}", componentKey);
                    }
                }
            }
        }
    }

}
