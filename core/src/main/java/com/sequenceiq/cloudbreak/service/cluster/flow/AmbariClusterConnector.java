package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.AMBARI_AGENT;
import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.AMBARI_DB;
import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.AMBARI_SERVER;
import static com.sequenceiq.cloudbreak.service.PollingResult.SUCCESS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.service.PollingResult.isTimeout;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.AMBARI_POLLING_INTERVAL;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.MAX_ATTEMPTS_FOR_HOSTS;
import static com.sequenceiq.cloudbreak.service.cluster.flow.RecipeEngine.DEFAULT_RECIPE_TIMEOUT;
import static java.util.Collections.singletonMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.AmbariConnectionException;
import com.sequenceiq.ambari.client.InvalidHostGroupHostAssociation;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.ClusterException;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.core.flow.FlowCancelledException;
import com.sequenceiq.cloudbreak.core.flow.service.AmbariHostsRemover;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.service.cluster.HadoopConfigurationService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.flow.TLSClientConfig;

import groovyx.net.http.HttpResponseException;

@Service
public class AmbariClusterConnector {

    private static final int MAX_ATTEMPTS_FOR_REGION_DECOM = 500;

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterConnector.class);

    @Inject
    private StackRepository stackRepository;
    @Inject
    private ClusterRepository clusterRepository;
    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;
    @Inject
    private HostGroupRepository hostGroupRepository;
    @Inject
    private AmbariOperationService ambariOperationService;
    @Inject
    private PollingService<AmbariHostsCheckerContext> hostsPollingService;
    @Inject
    private PollingService<AmbariHostsWithNames> rsPollerService;
    @Inject
    private HadoopConfigurationService hadoopConfigurationService;
    @Inject
    private AmbariClientProvider ambariClientProvider;
    @Inject
    private CloudbreakEventService eventService;
    @Inject
    private RecipeEngine recipeEngine;
    @Inject
    private PluginManager pluginManager;
    @Inject
    private AmbariHostsStatusCheckerTask ambariHostsStatusCheckerTask;
    @Inject
    private DNDecommissionStatusCheckerTask dnDecommissionStatusCheckerTask;
    @Inject
    private RSDecommissionStatusCheckerTask rsDecommissionStatusCheckerTask;
    @Inject
    private ClusterSecurityService securityService;
    @Inject
    private InstanceTerminationHandler instanceTerminationHandler;
    @Inject
    private AmbariHostsRemover ambariHostsRemover;
    @Inject
    private PollingService<AmbariHostsCheckerContext> ambariHostJoin;
    @Inject
    private PollingService<AmbariClientPollerObject> ambariHealthChecker;
    @Inject
    private AmbariHealthCheckerTask ambariHealthCheckerTask;
    @Inject
    private AmbariHostsJoinStatusCheckerTask ambariHostsJoinStatusCheckerTask;
    @Inject
    private TlsSecurityService tlsSecurityService;

    public Cluster buildAmbariCluster(Stack stack) {
        Cluster cluster = stack.getCluster();
        try {
            cluster.setCreationStarted(new Date().getTime());
            cluster = clusterRepository.save(cluster);
            TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
            AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(), cluster.getPassword());
            setBaseRepoURL(cluster, ambariClient);
            addBlueprint(stack, ambariClient, cluster.getBlueprint());
            int nodeCount = stack.getFullNodeCountWithoutDecommissionedNodes() - stack.getGateWayNodeCount();
            PollingResult waitForHostsResult = waitForHosts(stack, ambariClient, nodeCount);
            checkPollingResult(waitForHostsResult, "Error while waiting for hosts to connect. Polling result: " + waitForHostsResult.name());

            Set<HostGroup> hostGroups = hostGroupRepository.findHostGroupsInCluster(cluster.getId());
            Map<String, List<String>> hostGroupMappings = buildHostGroupAssociations(hostGroups);
            hostGroups = saveHostMetadata(cluster, hostGroupMappings);
            boolean recipesFound = recipesFound(hostGroups);
            if (recipesFound) {
                recipeEngine.setupRecipes(stack, hostGroups);
                recipeEngine.executePreInstall(stack);
            }
            ambariClient.createCluster(cluster.getName(), cluster.getBlueprint().getBlueprintName(), hostGroupMappings);
            PollingResult pollingResult = waitForClusterInstall(stack, ambariClient);
            checkPollingResult(pollingResult, "Cluster installation failed. Polling result: " + pollingResult.name());

            pollingResult = runSmokeTest(stack, ambariClient);
            checkPollingResult(pollingResult, "Ambari Smoke tests failed. Polling result: " + pollingResult.name());

            cluster = handleClusterCreationSuccess(stack, cluster);
            if (recipesFound) {
                recipeEngine.executePostInstall(stack);
            }
            return cluster;
        } catch (FlowCancelledException flowCancelledException) {
            throw flowCancelledException;
        } catch (Exception e) {
            LOGGER.error("Error while building the Ambari cluster. Message {}, throwable: {}", e.getMessage(), e);
            throw new AmbariOperationFailedException(e.getMessage(), e);
        }
    }

    private void checkPollingResult(PollingResult waitForHostsResult, String message) throws ClusterException {
        if (isExited(waitForHostsResult)) {
            throw new FlowCancelledException("Stack or cluster in delete in progress phase.");
        } else if (!isSuccess(waitForHostsResult)) {
            throw new ClusterException(message);
        }
    }

    public Set<String> installAmbariNode(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment, List<HostMetadata> hostMetadataNotUsed)
            throws CloudbreakSecuritySetupException {
        Set<String> result = new HashSet<>();
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stackId, cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getSecureAmbariClient(clientConfig, cluster);
        int nodeCount = stack.getFullNodeCountWithoutDecommissionedAndUnRegisteredNodes() - stack.getGateWayNodeCount()
                + hostGroupAdjustment.getScalingAdjustment();
        PollingResult pollingResult = waitForHosts(stack, ambariClient, nodeCount);
        if (SUCCESS.equals(pollingResult)) {
            HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), hostGroupAdjustment.getHostGroup());
            List<String> hosts = findFreeHosts(stack.getId(), hostGroup, hostGroupAdjustment.getScalingAdjustment());
            Set<HostGroup> hostGroupAsSet = Sets.newHashSet(hostGroup);
            Set<HostMetadata> hostMetadata = addHostMetadata(cluster, hosts, hostGroupAdjustment);
            if (recipesFound(hostGroupAsSet)) {
                recipeEngine.setupRecipesOnHosts(stack, hostGroup.getRecipes(), new HashSet<>(hostMetadata));
            }
            try {
                pollingResult = waitForAmbariOperations(stack, ambariClient, installServices(hosts, stack, ambariClient, hostGroupAdjustment));
                if (isSuccess(pollingResult)) {
                    pollingResult = waitForAmbariOperations(stack, ambariClient, singletonMap("START_SERVICES", ambariClient.startAllServices()));
                    if (isSuccess(pollingResult)) {
                        pollingResult = restartHadoopServices(stack, ambariClient, false);
                        if (isSuccess(pollingResult)) {
                            result.addAll(hosts);
                        }
                    }
                }
            } catch (Exception ex) {
                rollbackUpscaledInstances(stack, cluster, ambariClient, hostGroupAdjustment, hostMetadata);
            }
        }
        return result;
    }

    private void rollbackUpscaledInstances(Stack stack, Cluster cluster, AmbariClient ambariClient, HostGroupAdjustmentJson hostGroupAdjustment,
            Set<HostMetadata> hostMetadata) throws CloudbreakSecuritySetupException {
        Set<String> components = getHadoopComponents(cluster, ambariClient, hostGroupAdjustment.getHostGroup(),
                cluster.getBlueprint().getBlueprintName());
        Map<String, HostMetadata> hostsToRemove = new HashMap<>();
        for (HostMetadata data : hostMetadata) {
            hostsToRemove.put(data.getHostName(), data);
        }
        HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), hostGroupAdjustment.getHostGroup());
        List<String> hostList = new ArrayList<>(hostsToRemove.keySet());
        ambariHostsRemover.deleteHosts(stack, hostList, new ArrayList<>(components));
        PollingResult pollingResult = restartHadoopServices(stack, ambariClient, true);
        if (isSuccess(pollingResult)) {
            hostGroup.getHostMetadata().removeAll(hostsToRemove.values());
            hostGroupRepository.save(hostGroup);
            for (HostMetadata metadata : hostsToRemove.values()) {
                InstanceMetaData hostInStack = instanceMetadataRepository.findHostInStack(stack.getId(), metadata.getHostName());
                instanceTerminationHandler.terminateInstance(stack, hostInStack);
            }
        }
    }

    public AmbariClient getAmbariClientByStack(Stack stack) throws CloudbreakSecuritySetupException {
        Cluster cluster = stack.getCluster();
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        return ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(),
                cluster.getPassword());
    }

    public Cluster resetAmbariCluster(Long stackId) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findOneWithLists(stackId);
        InstanceGroup instanceGroupByType = stack.getGatewayInstanceGroup();
        Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        List<String> hostNames = instanceMetadataRepository.findAliveInstancesHostNamesInInstanceGroup(instanceGroupByType.getId());
        eventService.fireCloudbreakEvent(stackId, Status.UPDATE_IN_PROGRESS.name(), "Ambari database reset started.");
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.RESET_AMBARI_DB_EVENT, DEFAULT_RECIPE_TIMEOUT, AMBARI_DB,
                Collections.<String>emptyList(), new HashSet<>(hostNames));
        eventService.fireCloudbreakEvent(stackId, Status.UPDATE_IN_PROGRESS.name(), "Ambari database reset finished with success.");
        eventService.fireCloudbreakEvent(stackId, Status.UPDATE_IN_PROGRESS.name(), "Ambari server restart started.");
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.RESET_AMBARI_EVENT, DEFAULT_RECIPE_TIMEOUT, AMBARI_SERVER,
                Collections.<String>emptyList(), new HashSet<>(hostNames));
        eventService.fireCloudbreakEvent(stackId, Status.UPDATE_IN_PROGRESS.name(), "Ambari server restart finished with success.");
        return cluster;
    }

    public Set<String> decommissionAmbariNodes(Long stackId, HostGroupAdjustmentJson adjustmentRequest, List<HostMetadata> decommissionCandidates)
            throws CloudbreakSecuritySetupException {
        Set<String> result = new HashSet<>();
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = stack.getCluster();
        LOGGER.info("Decommission requested");
        int adjustment = Math.abs(adjustmentRequest.getScalingAdjustment());
        String hostGroupName = adjustmentRequest.getHostGroup();
        LOGGER.info("Decommissioning {} hosts from host group '{}'", adjustment, hostGroupName);
        String eventMsg = String.format("Removing '%s' node(s) from the '%s' hostgroup.", adjustment, hostGroupName);
        eventService.fireCloudbreakInstanceGroupEvent(stackId, Status.UPDATE_IN_PROGRESS.name(), eventMsg, hostGroupName);
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stackId, cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getSecureAmbariClient(clientConfig, cluster);
        String blueprintName = stack.getCluster().getBlueprint().getBlueprintName();
        Set<String> components = getHadoopComponents(cluster, ambariClient, hostGroupName, blueprintName);
        Map<String, HostMetadata> hostsToRemove = selectHostsToRemove(decommissionCandidates, stack, adjustment);
        List<String> hostList = new ArrayList<>(hostsToRemove.keySet());
        PollingResult pollingResult = waitForAmbariOperations(stack, ambariClient, decommissionComponents(ambariClient, hostList, components));
        if (isSuccess(pollingResult)) {
            pollingResult = waitForDataNodeDecommission(stack, ambariClient);
            if (isSuccess(pollingResult)) {
                pollingResult = waitForRegionServerDecommission(stack, ambariClient, hostList, components);
                if (isSuccess(pollingResult)) {
                    pollingResult = stopHadoopComponents(stack, ambariClient, hostList);
                    if (isSuccess(pollingResult)) {
                        ambariHostsRemover.deleteHosts(stack, hostList, new ArrayList<>(components));
                        pollingResult = restartHadoopServices(stack, ambariClient, true);
                        if (isSuccess(pollingResult)) {
                            cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
                            HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), hostGroupName);
                            hostGroup.getHostMetadata().removeAll(hostsToRemove.values());
                            hostGroupRepository.save(hostGroup);
                            result.addAll(hostsToRemove.keySet());
                        }
                    }
                }
            }
        }
        return result;
    }

    public void stopCluster(Stack stack) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(), cluster.getPassword());
        try {
            if (!allServiceStopped(ambariClient.getHostComponentsStates())) {
                stopAllServices(stack, ambariClient);
            }
            stopAmbariAgents(stack);
        } catch (AmbariConnectionException ex) {
            LOGGER.debug("Ambari not running on the gateway machine, no need to stop it.");
        }
    }

    public void startCluster(Stack stack) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(), cluster.getPassword());
        waitForAmbariToStart(stack);
        startAmbariAgents(stack);
        startAllServices(stack, ambariClient);
    }

    public boolean isAmbariAvailable(Stack stack) throws CloudbreakException {
        boolean result = false;
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
            AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(), cluster.getPassword());
            AmbariClientPollerObject ambariClientPollerObject = new AmbariClientPollerObject(stack, ambariClient);
            try {
                result = ambariHealthCheckerTask.checkStatus(ambariClientPollerObject);
            } catch (Exception ex) {
                result = false;
            }
        }
        return result;
    }

    public boolean deleteHostFromAmbari(Stack stack, HostMetadata data) throws CloudbreakSecuritySetupException {
        boolean hostDeleted = false;
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), stack.getCluster().getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getSecureAmbariClient(clientConfig, stack.getCluster());
        Set<String> components = getHadoopComponents(stack.getCluster(), ambariClient, data.getHostGroup().getName(),
                stack.getCluster().getBlueprint().getBlueprintName());
        if (ambariClient.getClusterHosts().contains(data.getHostName())) {
            String hostState = ambariClient.getHostState(data.getHostName());
            if ("UNKNOWN".equals(hostState)) {
                ambariHostsRemover.deleteHosts(stack, Arrays.asList(data.getHostName()), new ArrayList<>(components));
                PollingResult result = restartHadoopServices(stack, ambariClient, true);
                if (isTimeout(result)) {
                    throw new AmbariOperationFailedException("Timeout while restarting Hadoop services.");
                } else if (isExited(result)) {
                    throw new FlowCancelledException("Cluster was terminated while restarting Hadoop services.");
                }
                hostDeleted = true;
            }
        } else {
            LOGGER.debug("Host is already deleted.");
            hostDeleted = true;
        }
        return hostDeleted;
    }

    private void stopAllServices(Stack stack, AmbariClient ambariClient) throws CloudbreakException {
        int requestId = ambariClient.stopAllServices();
        if (requestId != -1) {
            LOGGER.info("Waiting for Hadoop services to stop on stack");
            PollingResult servicesStopResult = ambariOperationService.waitForAmbariOperations(stack, ambariClient, singletonMap("stop services", requestId));
            if (isExited(servicesStopResult)) {
                throw new FlowCancelledException("Cluster was terminated while waiting for Hadoop services to start");
            } else if (isTimeout(servicesStopResult)) {
                throw new CloudbreakException("Timeout while stopping Ambari services.");
            }
        } else {
            throw new CloudbreakException("Failed to stop Hadoop services.");
        }
    }

    private void startAllServices(Stack stack, AmbariClient ambariClient) throws CloudbreakException {
        int requestId = ambariClient.startAllServices();
        if (requestId != -1) {
            LOGGER.info("Waiting for Hadoop services to start on stack");
            PollingResult servicesStartResult = ambariOperationService.waitForAmbariOperations(stack, ambariClient, singletonMap("start services", requestId));
            if (isExited(servicesStartResult)) {
                throw new FlowCancelledException("Cluster was terminated while waiting for Hadoop services to start");
            } else if (isTimeout(servicesStartResult)) {
                throw new CloudbreakException("Timeout while starting Ambari services.");
            }
        } else {
            throw new CloudbreakException("Failed to start Hadoop services.");
        }
    }

    private Set<String> getHadoopComponents(Cluster cluster, AmbariClient ambariClient, String hostGroupName, String blueprintName) {
        Set<String> components = new HashSet<>(ambariClient.getComponentsCategory(blueprintName, hostGroupName).keySet());
        if (cluster.isSecure()) {
            components.add(ClusterSecurityService.KERBEROS_CLIENT);
        }
        return components;
    }

    private Cluster handleClusterCreationSuccess(Stack stack, Cluster cluster) {
        LOGGER.info("Cluster created successfully. Cluster name: {}", cluster.getName());
        cluster.setCreationFinished(new Date().getTime());
        cluster.setUpSince(new Date().getTime());
        cluster = clusterRepository.save(cluster);
        List<InstanceMetaData> updatedInstances = new ArrayList<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            Set<InstanceMetaData> instances = instanceGroup.getAllInstanceMetaData();
            for (InstanceMetaData instanceMetaData : instances) {
                if (!instanceMetaData.isTerminated()) {
                    instanceMetaData.setInstanceStatus(InstanceStatus.REGISTERED);
                    updatedInstances.add(instanceMetaData);
                }
            }
        }
        instanceMetadataRepository.save(updatedInstances);
        return cluster;

    }

    private List<String> getHostNames(Set<InstanceMetaData> instances) {
        return FluentIterable.from(instances).transform(new Function<InstanceMetaData, String>() {
            @Nullable
            @Override
            public String apply(@Nullable InstanceMetaData input) {
                return input.getDiscoveryFQDN();
            }
        }).toList();
    }

    private boolean recipesFound(Set<HostGroup> hostGroups) {
        for (HostGroup hostGroup : hostGroups) {
            if (!hostGroup.getRecipes().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private Map<String, HostMetadata> selectHostsToRemove(List<HostMetadata> decommissionCandidates, Stack stack, int adjustment) {
        Map<String, HostMetadata> hostsToRemove = new HashMap<>();
        int i = 0;
        for (HostMetadata hostMetadata : decommissionCandidates) {
            String hostName = hostMetadata.getHostName();
            InstanceMetaData instanceMetaData = instanceMetadataRepository.findHostInStack(stack.getId(), hostName);
            if (!instanceMetaData.getAmbariServer()) {
                if (i < adjustment) {
                    LOGGER.info("Host '{}' will be removed from Ambari cluster", hostName);
                    hostsToRemove.put(hostName, hostMetadata);
                } else {
                    break;
                }
                i++;
            }
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
            ambariClient.decommissionHBaseRegionServers(hosts);
            ambariClient.setHBaseRegionServersToMaintenance(hosts, true);
        }
        return decommissionRequests;
    }

    private PollingResult runSmokeTest(Stack stack, AmbariClient ambariClient) {
        int id = ambariClient.runMRServiceCheck();
        return waitForAmbariOperations(stack, ambariClient, singletonMap("MR_SMOKE_TEST", id));
    }

    private PollingResult stopHadoopComponents(Stack stack, AmbariClient ambariClient, List<String> hosts) {
        try {
            int requestId = ambariClient.stopAllComponentsOnHosts(hosts);
            return waitForAmbariOperations(stack, ambariClient, singletonMap("Stopping components on the decommissioned hosts", requestId));
        } catch (HttpResponseException e) {
            throw new AmbariOperationFailedException("Ambari client could not stop components.", e);
        }
    }

    private PollingResult restartHadoopServices(Stack stack, AmbariClient ambariClient, boolean decommissioned) {
        Map<String, Integer> restartRequests = new HashMap<>();
        Map<String, Map<String, String>> serviceComponents = ambariClient.getServiceComponentsMap();
        if (decommissioned) {
            int zookeeperRequestId = ambariClient.restartServiceComponents("ZOOKEEPER", Arrays.asList("ZOOKEEPER_SERVER"));
            restartRequests.put("ZOOKEEPER", zookeeperRequestId);
        }
        if (serviceComponents.containsKey("NAGIOS")) {
            restartRequests.put("NAGIOS", ambariClient.restartServiceComponents("NAGIOS", Arrays.asList("NAGIOS_SERVER")));
        }
        if (serviceComponents.containsKey("GANGLIA")) {
            restartRequests.put("GANGLIA", ambariClient.restartServiceComponents("GANGLIA", Arrays.asList("GANGLIA_SERVER")));
        }
        return waitForAmbariOperations(stack, ambariClient, restartRequests);
    }

    private void waitForAmbariToStart(Stack stack) throws CloudbreakException {
        LOGGER.info("Checking if Ambari Server is available.");
        Cluster cluster = stack.getCluster();
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        PollingResult ambariHealthCheckResult = ambariHealthChecker.pollWithTimeout(
                ambariHealthCheckerTask,
                new AmbariClientPollerObject(stack, ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(), cluster.getPassword())),
                AMBARI_POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_HOSTS,
                AmbariOperationService.MAX_FAILURE_COUNT);
        if (isExited(ambariHealthCheckResult)) {
            throw new FlowCancelledException("Cluster was terminated while waiting for Ambari to start.");
        } else if (isTimeout(ambariHealthCheckResult)) {
            throw new CloudbreakException("Ambari server was not restarted properly.");
        }
    }

    private void stopAmbariAgents(Stack stack) throws CloudbreakException {
        LOGGER.info("Stopping Ambari agents on the hosts.");
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.STOP_AMBARI_EVENT, DEFAULT_RECIPE_TIMEOUT, AMBARI_AGENT);
    }

    private void startAmbariAgents(Stack stack) throws CloudbreakException {
        LOGGER.info("Starting Ambari agents on the hosts.");
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.START_AMBARI_EVENT, DEFAULT_RECIPE_TIMEOUT, AMBARI_AGENT);
        PollingResult hostsJoinedResult = waitForHostsToJoin(stack);
        if (PollingResult.EXIT.equals(hostsJoinedResult)) {
            throw new FlowCancelledException("Cluster was terminated while starting Ambari agents.");
        } else if (PollingResult.TIMEOUT.equals(hostsJoinedResult)) {
            LOGGER.info("Ambari agents couldn't join. Restarting ambari agents...");
            restartAmbariAgents(stack);
        }
    }

    private void restartAmbariAgents(Stack stack) throws CloudbreakException {
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.RESTART_AMBARI_EVENT, DEFAULT_RECIPE_TIMEOUT, AMBARI_AGENT);
        PollingResult hostsJoinedResult = waitForHostsToJoin(stack);
        if (isExited(hostsJoinedResult)) {
            throw new FlowCancelledException("Cluster was terminated while restarting Ambari agents.");
        } else if (isTimeout(hostsJoinedResult)) {
            throw new CloudbreakException("Services could not start because host(s) could not join.");
        }
    }

    private PollingResult waitForHostsToJoin(Stack stack) throws CloudbreakSecuritySetupException {
        AmbariHostsCheckerContext ambariHostsCheckerContext = new AmbariHostsCheckerContext(stack, getAmbariClientByStack(stack), stack.getFullNodeCount());
        return ambariHostJoin.pollWithTimeout(
                ambariHostsJoinStatusCheckerTask,
                ambariHostsCheckerContext,
                AMBARI_POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_HOSTS,
                AmbariOperationService.MAX_FAILURE_COUNT);
    }

    private boolean allServiceStopped(Map<String, Map<String, String>> hostComponentsStates) {
        boolean stopped = true;
        Collection<Map<String, String>> values = hostComponentsStates.values();
        for (Map<String, String> value : values) {
            for (String state : value.values()) {
                if (!"INSTALLED".equals(state)) {
                    stopped = false;
                }
            }
        }
        return stopped;
    }

    private Set<HostGroup> saveHostMetadata(Cluster cluster, Map<String, List<String>> hostGroupMappings) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (Entry<String, List<String>> hostGroupMapping : hostGroupMappings.entrySet()) {
            Set<HostMetadata> hostMetadata = new HashSet<>();
            HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), hostGroupMapping.getKey());
            for (String hostName : hostGroupMapping.getValue()) {
                HostMetadata hostMetadataEntry = new HostMetadata();
                hostMetadataEntry.setHostName(hostName);
                hostMetadataEntry.setHostGroup(hostGroup);
                hostMetadata.add(hostMetadataEntry);
            }
            hostGroup.setHostMetadata(hostMetadata);
            hostGroups.add(hostGroupRepository.save(hostGroup));
        }
        return hostGroups;
    }

    private Set<HostMetadata> addHostMetadata(Cluster cluster, List<String> hosts, HostGroupAdjustmentJson hostGroupAdjustment) {
        Set<HostMetadata> hostMetadata = new HashSet<>();
        HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), hostGroupAdjustment.getHostGroup());
        for (String host : hosts) {
            HostMetadata hostMetadataEntry = new HostMetadata();
            hostMetadataEntry.setHostName(host);
            hostMetadataEntry.setHostGroup(hostGroup);
            hostMetadata.add(hostMetadataEntry);
        }
        hostGroup.getHostMetadata().addAll(hostMetadata);
        hostGroupRepository.save(hostGroup);
        return hostMetadata;
    }

    private void setBaseRepoURL(Cluster cluster, AmbariClient ambariClient) {
        AmbariStackDetails ambStack = cluster.getAmbariStackDetails();
        if (ambStack != null) {
            LOGGER.info("Use specific Ambari repository: {}", ambStack);
            try {
                String stack = ambStack.getStack();
                String version = ambStack.getVersion();
                String os = ambStack.getOs();
                boolean verify = ambStack.isVerify();
                addRepository(ambariClient, stack, version, os, ambStack.getStackRepoId(), ambStack.getStackBaseURL(), verify);
                addRepository(ambariClient, stack, version, os, ambStack.getUtilsRepoId(), ambStack.getUtilsBaseURL(), verify);
            } catch (HttpResponseException e) {
                throw new BadRequestException("Cannot use the specified Ambari stack: " + ambStack.toString(), e);
            }
        } else {
            LOGGER.info("Using latest HDP repository");
        }
    }

    private void addRepository(AmbariClient client, String stack, String version, String os,
            String repoId, String repoUrl, boolean verify) throws HttpResponseException {
        client.addStackRepository(stack, version, os, repoId, repoUrl, verify);
    }

    private void addBlueprint(Stack stack, AmbariClient ambariClient, Blueprint blueprint) {
        try {
            ambariClient.addBlueprintWithHostgroupConfiguration(blueprint.getBlueprintText(), hadoopConfigurationService.getConfiguration(stack));
            LOGGER.info("Blueprint added [Stack: {}, blueprint: '{}']", stack.getId(), blueprint.getId());
        } catch (HttpResponseException e) {
            if ("Conflict".equals(e.getMessage())) {
                throw new BadRequestException("Ambari blueprint already exists.", e);
            } else if ("Bad Request".equals(e.getMessage())) {
                throw new BadRequestException("Failed to validate Ambari blueprint.", e);
            } else {
                throw new CloudbreakServiceException(e);
            }
        }
    }

    private PollingResult waitForHosts(Stack stack, AmbariClient ambariClient, int nodeCount) {
        LOGGER.info("Waiting for hosts to connect.[Ambari server address: {}]", stack.getAmbariIp());
        return hostsPollingService.pollWithTimeout(
                ambariHostsStatusCheckerTask, new AmbariHostsCheckerContext(stack, ambariClient, nodeCount), AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_HOSTS);
    }

    private Map<String, List<String>> buildHostGroupAssociations(Set<HostGroup> hostGroups) throws InvalidHostGroupHostAssociation {
        Map<String, List<String>> hostGroupMappings = new HashMap<>();
        LOGGER.info("Computing host - hostGroup mappings based on hostGroup - instanceGroup associations");
        for (HostGroup hostGroup : hostGroups) {
            hostGroupMappings.put(hostGroup.getName(),
                    instanceMetadataRepository.findAliveInstancesHostNamesInInstanceGroup(hostGroup.getInstanceGroup().getId()));
        }
        LOGGER.info("Computed host-hostGroup associations: {}", hostGroupMappings);
        return hostGroupMappings;
    }

    private PollingResult waitForClusterInstall(Stack stack, AmbariClient ambariClient) {
        Map<String, Integer> clusterInstallRequest = new HashMap<>();
        clusterInstallRequest.put("CLUSTER_INSTALL", 1);
        return waitForAmbariOperations(stack, ambariClient, clusterInstallRequest);
    }

    private List<String> findFreeHosts(Long stackId, HostGroup hostGroup, int scalingAdjustment) {
        Set<InstanceMetaData> unregisteredHosts = instanceMetadataRepository.findUnregisteredHostsInInstanceGroup(hostGroup.getInstanceGroup().getId());
        Set<InstanceMetaData> instances = FluentIterable.from(unregisteredHosts).limit(scalingAdjustment).toSet();
        String statusReason = String.format("Adding '%s' new host(s) to the '%s' host group.", instances.size(), hostGroup.getName());
        eventService.fireCloudbreakInstanceGroupEvent(stackId, Status.UPDATE_IN_PROGRESS.name(), statusReason, hostGroup.getName());
        return getHostNames(instances);
    }

    private Map<String, Integer> installServices(List<String> hosts, Stack stack, AmbariClient ambariClient, HostGroupAdjustmentJson hostGroup) {
        try {
            Map<String, Integer> requests = new HashMap<>();
            Cluster cluster = stack.getCluster();
            ambariClient.addHosts(hosts);
            String blueprintName = cluster.getBlueprint().getBlueprintName();
            String hGroupName = hostGroup.getHostGroup();
            ambariClient.addComponentsToHosts(hosts, blueprintName, hGroupName);
            ambariClient.addHostsToConfigGroups(hosts, hGroupName);
            if (cluster.isSecure()) {
                ambariClient.addComponentsToHosts(hosts, Arrays.asList(securityService.KERBEROS_CLIENT));
            }
            requests.put("Install components to the new hosts", ambariClient.installAllComponentsOnHosts(hosts));
            if (cluster.isSecure()) {
                requests.put("Re-generate missing keytabs", ambariClient.generateKeytabs(true));
            }
            return requests;
        } catch (HttpResponseException e) {
            if ("Conflict".equals(e.getMessage())) {
                throw new BadRequestException("Host already exists.", e);
            } else if ("Bad Request".equals(e.getMessage())) {
                throw new BadRequestException("Failed to validate host.", e);
            } else {
                throw new CloudbreakServiceException(e);
            }
        }
    }

    private PollingResult startComponents(Stack stack, Cluster cluster, AmbariClient ambariClient, HostGroup hostGroup) {
        try {
            int id = ambariClient.startAllComponents(cluster.getBlueprint().getBlueprintName(), hostGroup.getName());
            return waitForAmbariOperations(stack, ambariClient, singletonMap("START_SERVICES on new hosts", id));
        } catch (HttpResponseException e) {
            throw new BadRequestException("Failed to start the components on the new hosts", e);
        }
    }

    private PollingResult waitForAmbariOperations(Stack stack, AmbariClient ambariClient, Map<String, Integer> operationRequests) {
        LOGGER.info("Waiting for Ambari operations to finish. [Operation requests: {}]", operationRequests);
        return ambariOperationService.waitForAmbariOperations(stack, ambariClient, operationRequests);
    }

    private PollingResult waitForDataNodeDecommission(Stack stack, AmbariClient ambariClient) {
        LOGGER.info("Waiting for DataNodes to move the blocks to other nodes. stack id: {}", stack.getId());
        return ambariOperationService.waitForAmbariOperations(stack, ambariClient, dnDecommissionStatusCheckerTask, Collections.<String, Integer>emptyMap());
    }

    private PollingResult waitForRegionServerDecommission(Stack stack, AmbariClient ambariClient, List<String> hosts, Set<String> components) {
        if (!components.contains("HBASE_REGIONSERVER")) {
            return SUCCESS;
        }
        LOGGER.info("Waiting for RegionServers to move the regions to other servers");
        return rsPollerService.pollWithTimeout(
                rsDecommissionStatusCheckerTask,
                new AmbariHostsWithNames(stack, ambariClient, hosts),
                AMBARI_POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_REGION_DECOM);
    }
}
