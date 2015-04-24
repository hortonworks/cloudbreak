package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.service.PollingResult.SUCCESS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.service.cluster.flow.DockerContainer.AMBARI_DB;
import static com.sequenceiq.cloudbreak.service.cluster.flow.DockerContainer.AMBARI_SERVER;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.InvalidHostGroupHostAssociation;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.core.ClusterException;
import com.sequenceiq.cloudbreak.core.flow.service.AmbariHostsRemover;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.service.cluster.HadoopConfigurationService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;

import groovyx.net.http.HttpResponseException;

@Service
public class AmbariClusterConnector {

    private static final int MAX_ATTEMPTS_FOR_REGION_DECOM = 500;

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterConnector.class);

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private ClusterRepository clusterRepository;
    @Autowired
    private InstanceMetaDataRepository instanceMetadataRepository;
    @Autowired
    private HostGroupRepository hostGroupRepository;
    @Autowired
    private RetryingStackUpdater stackUpdater;
    @Autowired
    private AmbariOperationService ambariOperationService;
    @Autowired
    private PollingService<AmbariHosts> hostsPollingService;
    @Autowired
    private PollingService<AmbariHostsWithNames> rsPollerService;
    @Autowired
    private HadoopConfigurationService hadoopConfigurationService;
    @Autowired
    private AmbariClientProvider ambariClientProvider;
    @Autowired
    private CloudbreakEventService eventService;
    @Autowired
    private RecipeEngine recipeEngine;
    @Autowired
    private PluginManager pluginManager;

    @Autowired
    private AmbariHostsStatusCheckerTask ambariHostsStatusCheckerTask;
    @Autowired
    private DNDecommissionStatusCheckerTask dnDecommissionStatusCheckerTask;
    @Autowired
    private RSDecommissionStatusCheckerTask rsDecommissionStatusCheckerTask;
    @Autowired
    private ClusterSecurityService securityService;

    @Autowired
    private AmbariHostsRemover ambariHostsRemover;

    public Cluster buildAmbariCluster(Stack stack) {
        Cluster cluster = stack.getCluster();
        try {
            LOGGER.info("Starting Ambari cluster [Ambari server address: {}]", stack.getAmbariIp());
            stack = stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS, "Building the Ambari cluster.");
            cluster.setCreationStarted(new Date().getTime());
            cluster = clusterRepository.save(cluster);

            AmbariClient ambariClient = ambariClientProvider.getAmbariClient(stack.getAmbariIp(), stack.getUserName(), stack.getPassword());
            addBlueprint(stack, ambariClient, cluster.getBlueprint());
            PollingResult waitForHostsResult = waitForHosts(stack, ambariClient);

            if (!isSuccess(waitForHostsResult)) {
                throw new ClusterException("Error while waiting for hosts to connect. Polling result: " + waitForHostsResult.name());
            }

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

            if (!isSuccess(pollingResult)) {
                throw new ClusterException("Cluster installation failed. Polling result: " + pollingResult.name());
            }

            pollingResult = runSmokeTest(stack, ambariClient);
            if (!isSuccess(pollingResult)) {
                throw new ClusterException("Ambari Smoke tests failed. Polling result: " + pollingResult.name());
            }

            cluster = handleClusterCreationSuccess(stack, cluster);
            if (recipesFound) {
                recipeEngine.executePostInstall(stack);
            }
            return cluster;
        } catch (Exception e) {
            LOGGER.error("Error while building the Ambari cluster. Message {}, throwable: {}", e.getMessage(), e);
            throw new AmbariOperationFailedException(e.getMessage(), e);
        }
    }

    public Set<String> installAmbariNode(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) {
        Set<String> result = new HashSet<>();
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS, "Adding new host(s) to the cluster.");
        AmbariClient ambariClient = ambariClientProvider.getSecureAmbariClient(stack);
        if (PollingResult.SUCCESS.equals(waitForHosts(stack, ambariClient))) {
            HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), hostGroupAdjustment.getHostGroup());
            List<String> hosts = findFreeHosts(stack.getId(), hostGroup, hostGroupAdjustment.getScalingAdjustment());
            Set<HostGroup> hostGroupAsSet = Sets.newHashSet(hostGroup);
            Set<HostMetadata> hostMetadata = addHostMetadata(cluster, hosts, hostGroupAdjustment);
            if (recipesFound(hostGroupAsSet)) {
                recipeEngine.setupRecipesOnHosts(stack, hostGroup.getRecipes(), hostMetadata);
            }
            PollingResult pollingResult = waitForAmbariOperations(stack, ambariClient, installServices(hosts, stack, ambariClient, hostGroupAdjustment));
            if (isSuccess(pollingResult)) {
                pollingResult = startComponents(stack, cluster, ambariClient, hostGroup);
                if (isSuccess(pollingResult)) {
                    pollingResult = restartHadoopServices(stack, ambariClient, false);
                    if (isSuccess(pollingResult)) {
                        result.addAll(hosts);
                    }
                }
            }
        }
        return result;
    }

    public Cluster resetAmbariCluster(Long stackId) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        InstanceGroup instanceGroupByType = stack.getInstanceGroupByType(InstanceGroupType.GATEWAY);
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

    public Set<String> decommissionAmbariNodes(Long stackId, HostGroupAdjustmentJson adjustmentRequest, List<HostMetadata> decommissionCandidates) {
        Set<String> result = new HashSet<>();
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = stack.getCluster();
        LOGGER.info("Decommission requested");
        int adjustment = Math.abs(adjustmentRequest.getScalingAdjustment());
        String hostGroupName = adjustmentRequest.getHostGroup();
        LOGGER.info("Decommissioning {} hosts from host group '{}'", adjustment, hostGroupName);
        String statusReason = String.format("Removing '%s' node(s) from the cluster.", adjustment);
        stackUpdater.updateStackStatus(stackId, Status.UPDATE_IN_PROGRESS, statusReason);
        String eventMsg = String.format("Removing '%s' node(s) from the '%s' hostgroup.", adjustment, hostGroupName);
        eventService.fireCloudbreakInstanceGroupEvent(stackId, Status.UPDATE_IN_PROGRESS.name(), eventMsg, hostGroupName);
        AmbariClient ambariClient = ambariClientProvider.getSecureAmbariClient(stack);
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

    public boolean stopCluster(Stack stack) {
        return setClusterState(stack, true);
    }

    public boolean startCluster(Stack stack) {
        return setClusterState(stack, false);
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
        cluster.setStatus(Status.AVAILABLE);
        cluster.setStatusReason("");
        cluster.setCreationFinished(new Date().getTime());
        cluster.setUpSince(new Date().getTime());
        cluster = clusterRepository.save(cluster);
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            Set<InstanceMetaData> instances = instanceGroup.getAllInstanceMetaData();
            for (InstanceMetaData instanceMetaData : instances) {
                if (!instanceMetaData.isTerminated()) {
                    instanceMetaData.setInstanceStatus(InstanceStatus.REGISTERED);
                }
            }
            stackUpdater.updateStackMetaData(stack.getId(), instances, instanceGroup.getGroupName());
        }
        stackUpdater.updateStackStatus(stack.getId(),
                Status.AVAILABLE, "Cluster installation successfully finished. AMBARI_IP:" + stack.getAmbariIp());

        return cluster;

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

    private boolean setClusterState(Stack stack, boolean stopped) {
        boolean result = true;
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(stack.getAmbariIp(), stack.getUserName(), stack.getPassword());
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
            PollingResult pollingResult = ambariOperationService.waitForAmbariOperations(stack, ambariClient, singletonMap(action + " services", requestId));
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
                throw new InternalServerException("Something went wrong", e);
            }
        }
    }

    private PollingResult waitForHosts(Stack stack, AmbariClient ambariClient) {
        LOGGER.info("Waiting for hosts to connect.[Ambari server address: {}]", stack.getAmbariIp());
        return hostsPollingService.pollWithTimeout(
                ambariHostsStatusCheckerTask,
                new AmbariHosts(stack, ambariClient, stack.getFullNodeCountWithoutDecommissionedNodes() - stack.getGateWayNodeCount()),
                AmbariOperationService.AMBARI_POLLING_INTERVAL,
                AmbariOperationService.MAX_ATTEMPTS_FOR_HOSTS);
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

    private PollingResult waitForClusterInstall(Stack stack, AmbariClient ambariClient) {
        Map<String, Integer> clusterInstallRequest = new HashMap<>();
        clusterInstallRequest.put("CLUSTER_INSTALL", 1);
        return waitForAmbariOperations(stack, ambariClient, clusterInstallRequest);
    }

    private List<String> findFreeHosts(Long stackId, HostGroup hostGroup, int scalingAdjustment) {
        Set<InstanceMetaData> unregisteredHosts = instanceMetadataRepository.findUnregisteredHostsInInstanceGroup(hostGroup.getInstanceGroup().getId());
        Set<InstanceMetaData> instances = FluentIterable.from(unregisteredHosts).limit(scalingAdjustment).toSet();
        String statusReason = String.format("Adding '%s' new host(s) to the '%s' host group.", scalingAdjustment, hostGroup.getName());
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
                throw new InternalServerException("Something went wrong", e);
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
                AmbariOperationService.AMBARI_POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_REGION_DECOM);
    }
}
