package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.InvalidHostGroupHostAssociation;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.core.flow.ProvisioningContextFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientService;
import com.sequenceiq.cloudbreak.service.cluster.HadoopConfigurationService;
import com.sequenceiq.cloudbreak.service.cluster.filter.HostFilterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.status.AmbariClusterStatusUpdater;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;

import groovyx.net.http.HttpResponseException;

@Service
public class AmbariClusterConnector {

    public static final int POLLING_INTERVAL = 5000;
    public static final int MAX_ATTEMPTS_FOR_AMBARI_OPS = -1;
    public static final int MAX_ATTEMPTS_FOR_HOSTS = 240;

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterConnector.class);
    private static final String UNHANDLED_EXCEPTION_MSG = "Unhandled exception occurred while installing Ambari services.";

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private HostMetadataRepository hostMetadataRepository;

    @Autowired
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private PollingService<AmbariOperations> operationsPollingService;

    @Autowired
    private PollingService<AmbariHosts> hostsPollingService;

    @Autowired
    private HadoopConfigurationService hadoopConfigurationService;

    @Autowired
    private AmbariClientService clientService;

    @Autowired
    private HostFilterService hostFilterService;

    @Autowired
    private CloudbreakEventService eventService;

    @Autowired
    private AmbariClusterStatusUpdater clusterStatusUpdater;

    @Autowired
    private RecipeEngine recipeEngine;

    @Autowired
    private AmbariHostsStatusCheckerTask ambariHostsStatusCheckerTask;

    @Autowired
    private AmbariOperationsStatusCheckerTask ambariOperationsStatusCheckerTask;

    @Autowired
    private DNDecommissionStatusCheckerTask dnDecommissionStatusCheckerTask;

    public Object buildAmbariCluster(Stack stack) throws Exception {
        Object retVal = null;
        Cluster cluster = stack.getCluster();
        MDCBuilder.buildMdcContext(cluster);
        LOGGER.info("Starting Ambari cluster setup [Ambari server address: {}]", stack.getAmbariIp());
        stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS, "Building of cluster has been started.");
        cluster.setCreationStarted(new Date().getTime());
        cluster = clusterRepository.save(cluster);
        Blueprint blueprint = cluster.getBlueprint();

        AmbariClient ambariClient = clientService.create(stack);
        if (cluster.getRecipe() != null) {
            recipeEngine.setupRecipe(stack);
            recipeEngine.executePreInstall(stack);
        }
        addBlueprint(stack, ambariClient, blueprint);
        Map<String, List<String>> hostGroupMappings = recommend(stack, ambariClient);
        saveHostMetadata(cluster, hostGroupMappings);
        ambariClient.createCluster(cluster.getName(), blueprint.getBlueprintName(), hostGroupMappings);
        PollingResult pollingResult = waitForClusterInstall(stack, ambariClient);
        if (isSuccess(pollingResult)) {
            pollingResult = runSmokeTest(stack, ambariClient);
            if (!isExited(pollingResult)) {
                if (cluster.getRecipe() != null) {
                    recipeEngine.executePostInstall(stack);
                }
            }
            if (isSuccess(pollingResult)) {
                retVal = ProvisioningContextFactory.createClusterCreateSuccessContext(cluster.getId(), new Date().getTime(), stack.getAmbariIp());
            }
        }
        return retVal;
    }

    public Object installAmbariNode(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) {
        Object retVal = null;
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        MDCBuilder.buildMdcContext(cluster);
        stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS, "Adding new host(s) to the cluster.");
        AmbariClient ambariClient = clientService.create(stack);
        waitForHosts(stack, ambariClient);
        List<String> hosts = findFreeHosts(stack.getId(), hostGroupAdjustment);
        if (cluster.getRecipe() != null) {
            recipeEngine.setupRecipe(stack);
        }
        addHostMetadata(cluster, hosts, hostGroupAdjustment);
        PollingResult pollingResult = waitForAmbariOperations(stack, ambariClient, installServices(hosts, stack, ambariClient, hostGroupAdjustment));
        if (isSuccess(pollingResult)) {
            pollingResult = waitForAmbariOperations(stack, ambariClient, singletonMap("START_SERVICES", ambariClient.startAllServices()));
            if (isSuccess(pollingResult)) {
                pollingResult = restartHadoopServices(stack, ambariClient, false);
                if (isSuccess(pollingResult)) {
                    retVal = updateHostSuccessful(cluster, new HashSet<>(hosts), false);
                }
            }
        }
        return retVal;
    }

    private List<String> getHostNames(Set<InstanceMetaData> instances) {
        return FluentIterable.from(instances).transform(new Function<InstanceMetaData, String>() {
            @Nullable
            @Override
            public String apply(@Nullable InstanceMetaData input) {
                return input.getLongName();
            }
        }).toList();
    }

    public Object decommissionAmbariNodes(Long stackId, HostGroupAdjustmentJson adjustmentRequest, List<HostMetadata> decommissionCandidates)
            throws Exception {
        Object retVal = null;
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = stack.getCluster();
        MDCBuilder.buildMdcContext(cluster);
        LOGGER.info("Decommission requested");
        int adjustment = Math.abs(adjustmentRequest.getScalingAdjustment());
        String hostGroup = adjustmentRequest.getHostGroup();
        LOGGER.info("Decommissioning {} hosts from host group '{}'", adjustment, hostGroup);
        String statusReason = String.format("Removing '%s' node(s) from the cluster.", adjustment);
        stackUpdater.updateStackStatus(stackId, Status.UPDATE_IN_PROGRESS, statusReason);
        String eventMsg = String.format("Removing '%s' node(s) from the '%s' hostgroup.", adjustment, hostGroup);
        eventService.fireCloudbreakInstanceGroupEvent(stackId, Status.UPDATE_IN_PROGRESS.name(), eventMsg, hostGroup);
        AmbariClient ambariClient = clientService.create(stack);
        String blueprintName = stack.getCluster().getBlueprint().getBlueprintName();
        Set<String> components = ambariClient.getComponentsCategory(blueprintName, hostGroup).keySet();
        Map<String, HostMetadata> hostsToRemove = selectHostsToRemove(decommissionCandidates, stack, adjustment);
        List<String> hostList = new ArrayList<>(hostsToRemove.keySet());
        PollingResult pollingResult = waitForAmbariOperations(stack, ambariClient, decommissionComponents(ambariClient, hostList, components));
        if (isSuccess(pollingResult)) {
            pollingResult = waitForDataNodeDecommission(stack, ambariClient);
            if (isSuccess(pollingResult)) {
                pollingResult = stopHadoopComponents(stack, ambariClient, hostList);
                if (isSuccess(pollingResult)) {
                    deleteHostsFromAmbari(ambariClient, hostList, new ArrayList<>(components));
                    pollingResult = restartHadoopServices(stack, ambariClient, true);
                    if (isSuccess(pollingResult)) {
                        cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
                        cluster.getHostMetadata().removeAll(hostsToRemove.values());
                        clusterRepository.save(cluster);
                        retVal = updateHostSuccessful(cluster, hostsToRemove.keySet(), true);
                    }
                }
            }
        }
        return retVal;

    }

    public boolean stopCluster(Stack stack) {
        return setClusterState(stack, true);
    }

    public boolean startCluster(Stack stack) {
        return setClusterState(stack, false);
    }

    private Map<String, HostMetadata> selectHostsToRemove(List<HostMetadata> decommissionCandidates, Stack stack, int adjustment) {
        MDCBuilder.buildMdcContext(stack);
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
            int requestId = ambariClient.decommissionHBaseRegionServers(hosts);
            decommissionRequests.put("HBASE_REGIONSERVER_DECOMMISSION", requestId);
        }
        return decommissionRequests;
    }

    private PollingResult runSmokeTest(Stack stack, AmbariClient ambariClient) {
        int id = ambariClient.runMRServiceCheck();
        return waitForAmbariOperations(stack, ambariClient, singletonMap("MR_SMOKE_TEST", id));
    }

    private PollingResult stopHadoopComponents(Stack stack, AmbariClient ambariClient, List<String> hosts) throws HttpResponseException {
        int requestId = ambariClient.stopAllComponentsOnHosts(hosts);
        return waitForAmbariOperations(stack, ambariClient, singletonMap("Stopping components on the decommissioned hosts", requestId));
    }

    private void deleteHostsFromAmbari(AmbariClient ambariClient, List<String> hosts, List<String> components) {
        for (String hostName : hosts) {
            ambariClient.deleteHostComponents(hostName, components);
            ambariClient.deleteHost(hostName);
            ambariClient.unregisterHost(hostName);
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

    private boolean setClusterState(Stack stack, boolean stopped) {
        MDCBuilder.buildMdcContext(stack);
        boolean result = true;
        AmbariClient ambariClient = clientService.create(stack);
        String action = stopped ? "stop" : "start";
        int requestId = -1;
        try {
            if (stopped) {
                if (!allServiceStopped(ambariClient.getHostComponentsStates())) {
                    requestId = ambariClient.stopAllServices();
                } else {
                    requestId = -1;
                }
            } else {
                requestId = ambariClient.startAllServices();
            }
        } catch (Exception e) {
            LOGGER.warn(String.format("Failed to %s Hadoop services", action), e);
            result = false;
        }
        if (requestId != -1) {
            LOGGER.info("Waiting for Hadoop services to {} on stack", action);
            PollingResult pollingResult = operationsPollingService.pollWithTimeout(
                    ambariOperationsStatusCheckerTask,
                    new AmbariOperations(stack, ambariClient, singletonMap(action + " services", requestId)),
                    AmbariClusterConnector.POLLING_INTERVAL,
                    AmbariClusterConnector.MAX_ATTEMPTS_FOR_AMBARI_OPS);
            if (!isSuccess(pollingResult)) {
                result = false;
            }
        }
        return result;
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

    private void saveHostMetadata(Cluster cluster, Map<String, List<String>> hostGroupMappings) {
        Set<HostMetadata> hostMetadata = new HashSet<>();
        for (Entry<String, List<String>> hostGroupMapping : hostGroupMappings.entrySet()) {
            for (String hostName : hostGroupMapping.getValue()) {
                HostMetadata hostMetadataEntry = new HostMetadata();
                hostMetadataEntry.setHostName(hostName);
                hostMetadataEntry.setHostGroup(hostGroupMapping.getKey());
                hostMetadataEntry.setCluster(cluster);
                hostMetadata.add(hostMetadataEntry);
            }
        }
        cluster.setHostMetadata(hostMetadata);
        clusterRepository.save(cluster);
    }

    private void addBlueprint(Stack stack, AmbariClient ambariClient, Blueprint blueprint) {
        MDCBuilder.buildMdcContext(stack);
        try {
            ambariClient.addBlueprintWithHostgroupConfiguration(blueprint.getBlueprintText(), hadoopConfigurationService.getConfiguration(stack));
            LOGGER.info("Blueprint added [Stack: {}, blueprint: '{}']", stack.getId(), blueprint.getId());
        } catch (HttpResponseException e) {
            if ("Conflict".equals(e.getMessage())) {
                throw new BadRequestException("Ambari blueprint already exists.", e);
            } else if ("Bad Request".equals(e.getMessage())) {
                throw new BadRequestException("Failed to validate Ambari blueprint.", e);
            } else {
                throw new InternalServerException("Something went wrong", e);
            }
        }
    }

    private PollingResult waitForHosts(Stack stack, AmbariClient ambariClient) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Waiting for hosts to connect.[Ambari server address: {}]", stack.getAmbariIp());
        return hostsPollingService.pollWithTimeout(
                ambariHostsStatusCheckerTask,
                new AmbariHosts(stack, ambariClient, stack.getFullNodeCount()),
                POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_HOSTS);
    }

    private Map<String, List<String>> recommend(Stack stack, AmbariClient ambariClient) throws InvalidHostGroupHostAssociation {
        MDCBuilder.buildMdcContext(stack);
        Map<String, List<String>> hostGroupMappings = new HashMap<>();
        PollingResult pollingResult = waitForHosts(stack, ambariClient);
        if (isSuccess(pollingResult)) {
            LOGGER.info("Asking Ambari client to recommend host-hostGroup mapping [Ambari server address: {}]", stack.getAmbariIp());
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                for (InstanceMetaData metaData : instanceGroup.getInstanceMetaData()) {
                    if (hostGroupMappings.get(instanceGroup.getGroupName()) == null) {
                        hostGroupMappings.put(instanceGroup.getGroupName(), new ArrayList<String>());
                    }
                    hostGroupMappings.get(instanceGroup.getGroupName()).add(metaData.getLongName());
                }
            }
            LOGGER.info("recommended host-hostGroup mappings for stack: {}", hostGroupMappings);
        }
        return hostGroupMappings;
    }

    private void addHostMetadata(Cluster cluster, List<String> hosts, HostGroupAdjustmentJson hostGroupAdjustment) {
        Set<HostMetadata> hostMetadata = new HashSet<>();
        String hostGroup = hostGroupAdjustment.getHostGroup();
        for (String host : hosts) {
            HostMetadata hostMetadataEntry = new HostMetadata();
            hostMetadataEntry.setHostName(host);
            hostMetadataEntry.setHostGroup(hostGroup);
            hostMetadataEntry.setCluster(cluster);
            hostMetadata.add(hostMetadataEntry);
        }
        cluster.getHostMetadata().addAll(hostMetadata);
        clusterRepository.save(cluster);
    }

    private PollingResult waitForClusterInstall(Stack stack, AmbariClient ambariClient) {
        Map<String, Integer> clusterInstallRequest = new HashMap<>();
        clusterInstallRequest.put("CLUSTER_INSTALL", 1);
        return waitForAmbariOperations(stack, ambariClient, clusterInstallRequest);
    }

    private List<String> findFreeHosts(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) {
        Set<InstanceMetaData> unregisteredHosts = instanceMetadataRepository.findUnregisteredHostsInStack(stackId);
        Set<InstanceMetaData> instances = FluentIterable.from(unregisteredHosts).limit(hostGroupAdjustment.getScalingAdjustment()).toSet();
        String statusReason = String.format("Adding '%s' new host(s) to the '%s' host group.",
                hostGroupAdjustment.getScalingAdjustment(), hostGroupAdjustment.getHostGroup());
        eventService.fireCloudbreakInstanceGroupEvent(stackId, Status.UPDATE_IN_PROGRESS.name(), statusReason, hostGroupAdjustment.getHostGroup());
        return getHostNames(instances);
    }

    private Map<String, Integer> installServices(List<String> hosts, Stack stack, AmbariClient ambariClient, HostGroupAdjustmentJson hostGroup) {
        MDCBuilder.buildMdcContext(stack);
        try {
            ambariClient.addHosts(hosts);
            int requestId = ambariClient.installComponentsToHosts(hosts, stack.getCluster().getBlueprint().getBlueprintName(), hostGroup.getHostGroup());
            LOGGER.info("Request is sent to install the host group components. Ambari server: {}", stack.getAmbariIp());
            return singletonMap("Install components to the new hosts", requestId);
        } catch (HttpResponseException e) {
            if ("Conflict".equals(e.getMessage())) {
                throw new BadRequestException("Host already exists.", e);
            } else if ("Bad Request".equals(e.getMessage())) {
                throw new BadRequestException("Failed to validate host.", e);
            } else {
                throw new InternalServerException("Something went wrong", e);
            }
        }
    }

    private PollingResult waitForAmbariOperations(Stack stack, AmbariClient ambariClient, Map<String, Integer> operationRequests) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Waiting for Ambari operations to finish. [Operation requests: {}]", operationRequests);
        return waitForAmbariOperations(stack, ambariClient, ambariOperationsStatusCheckerTask, operationRequests);
    }

    private PollingResult waitForAmbariOperations(Stack stack, AmbariClient ambariClient, StatusCheckerTask task, Map<String, Integer> operationRequests) {
        return operationsPollingService.pollWithTimeout(
                task,
                new AmbariOperations(stack, ambariClient, operationRequests),
                POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_AMBARI_OPS);
    }

    private PollingResult waitForDataNodeDecommission(Stack stack, AmbariClient ambariClient) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Waiting for DataNodes to move the blocks to other nodes");
        return waitForAmbariOperations(stack, ambariClient, dnDecommissionStatusCheckerTask, Collections.<String, Integer>emptyMap());
    }

    private Object updateHostSuccessful(Cluster cluster, Set<String> hostNames, boolean decommission) {
        MDCBuilder.buildMdcContext(cluster);
        LOGGER.info("Publishing {} event", ReactorConfig.UPDATE_AMBARI_HOSTS_SUCCESS_EVENT);
        //reactor.notify(ReactorConfig.UPDATE_AMBARI_HOSTS_SUCCESS_EVENT, Event.wrap(new UpdateAmbariHostsSuccess(cluster.getId(), hostNames, decommission)));
        return ProvisioningContextFactory.createAmbariHostsUpdatedsSuccessContext(cluster.getId(), hostNames, decommission);
    }

    private Object updateHostFailed(Cluster cluster, String message, boolean addingNodes) {
        MDCBuilder.buildMdcContext(cluster);
        LOGGER.info("Publishing {} event", ReactorConfig.UPDATE_AMBARI_HOSTS_FAILED_EVENT);
        //reactor.notify(ReactorConfig.UPDATE_AMBARI_HOSTS_FAILED_EVENT, Event.wrap(new UpdateAmbariHostsFailure(cluster.getId(), message, addingNodes)));
        return ProvisioningContextFactory.createAmbariHostsUpdatedsFailureContext(cluster.getId(), message, addingNodes);
    }

}
