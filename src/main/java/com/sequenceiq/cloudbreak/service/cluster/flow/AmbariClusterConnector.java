package com.sequenceiq.cloudbreak.service.cluster.flow;

import static java.util.Collections.singletonMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.InvalidHostGroupHostAssociation;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
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
import com.sequenceiq.cloudbreak.service.cluster.AmbariHostsUnavailableException;
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.service.cluster.HadoopConfigurationService;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterCreationFailure;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterCreationSuccess;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsFailure;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsSuccess;
import com.sequenceiq.cloudbreak.service.cluster.filter.AmbariHostFilterService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;

import groovyx.net.http.HttpResponseException;
import reactor.core.Reactor;
import reactor.event.Event;

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
    private Reactor reactor;

    @Autowired
    private PollingService<AmbariOperationsPollerObject> operationsPollingService;

    @Autowired
    private PollingService<AmbariHostsPollerObject> hostsPollingService;

    @Autowired
    private HadoopConfigurationService hadoopConfigurationService;

    @Autowired
    private AmbariClientService clientService;

    @Autowired
    private AmbariHostFilterService hostFilterService;

    @Autowired
    private CloudbreakEventService eventService;

    @Autowired
    private AmbariHostsStatusCheckerTask ambariHostsStatusCheckerTask;

    @Autowired
    private AmbariOperationsStatusCheckerTask ambariOperationsStatusCheckerTask;

    @Autowired
    private DNDecommissionStatusCheckerTask dnDecommissionStatusCheckerTask;

    public void installAmbariCluster(Stack stack) {
        Cluster cluster = stack.getCluster();
        MDCBuilder.buildMdcContext(cluster);
        try {
            LOGGER.info("Starting Ambari cluster installation [Ambari server address: {}]", stack.getAmbariIp());
            stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS, "Installation of cluster has been started.");
            cluster.setCreationStarted(new Date().getTime());
            cluster = clusterRepository.save(cluster);
            Blueprint blueprint = cluster.getBlueprint();
            AmbariClient ambariClient = clientService.create(stack);

            addBlueprint(stack, ambariClient, blueprint);
            PollingResult pollingResult = waitForHosts(stack, ambariClient);
            if (PollingResult.SUCCESS.equals(pollingResult)) {
                Map<String, List<String>> hostGroupMappings = recommend(stack, ambariClient, blueprint.getBlueprintName());
                saveHostMetadata(cluster, hostGroupMappings);
                ambariClient.createCluster(cluster.getName(), blueprint.getBlueprintName(), hostGroupMappings);
                waitForClusterInstall(stack, ambariClient);
                runSmokeTest(stack, ambariClient);
                clusterCreateSuccess(cluster, new Date().getTime(), stack.getAmbariIp());
            }
        } catch (AmbariHostsUnavailableException | AmbariOperationFailedException | InvalidHostGroupHostAssociation e) {
            LOGGER.error(e.getMessage(), e);
            clusterCreateFailed(stack, cluster, e.getMessage());
        } catch (Exception e) {
            LOGGER.error(UNHANDLED_EXCEPTION_MSG, e);
            clusterCreateFailed(stack, cluster, UNHANDLED_EXCEPTION_MSG);
        }
    }

    public void installAmbariNode(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        MDCBuilder.buildMdcContext(cluster);
        try {
            stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS, "Adding new host(s) to the cluster.");
            AmbariClient ambariClient = clientService.create(stack);
            waitForHosts(stack, ambariClient);
            Map<String, String> hosts = findHosts(stack.getId(), hostGroupAdjustment);
            addHostMetadata(cluster, hosts);
            PollingResult pollingResult = waitForAmbariOperations(stack, ambariClient, installServices(hosts, stack, ambariClient));
            if (PollingResult.SUCCESS.equals(pollingResult)) {
                pollingResult = waitForAmbariOperations(stack, ambariClient, singletonMap("START_SERVICES", ambariClient.startAllServices()));
                if (PollingResult.SUCCESS.equals(pollingResult)) {
                    pollingResult = restartHadoopServices(stack, ambariClient, false);
                    if (PollingResult.SUCCESS.equals(pollingResult)) {
                        updateHostSuccessful(cluster, hosts.keySet(), false);
                    }
                }
            }
        } catch (AmbariHostsUnavailableException | AmbariOperationFailedException e) {
            LOGGER.error(e.getMessage(), e);
            updateHostFailed(cluster, e.getMessage(), true);
        } catch (Exception e) {
            LOGGER.error(UNHANDLED_EXCEPTION_MSG, e);
            updateHostFailed(cluster, UNHANDLED_EXCEPTION_MSG, true);
        }
    }

    public void decommissionAmbariNodes(Long stackId, int scalingAdjustment, List<HostMetadata> decommissionCandidates) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = stack.getCluster();
        MDCBuilder.buildMdcContext(cluster);
        LOGGER.info("Decommission requested");
        try {
            int adjustment = Math.abs(scalingAdjustment);
            LOGGER.info("Decommissioning {} hosts", adjustment);
            String statusReason = String.format("Removing '%s' node(s) from the cluster.", scalingAdjustment);
            stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS, statusReason);
            AmbariClient ambariClient = clientService.create(stack);
            Set<HostMetadata> metadataToRemove = new HashSet<>();
            Map<String, Integer> decommissionRequests = new HashMap<>();
            Map<String, List<String>> hostsWithComponents = new HashMap<>();
            int i = 0;
            for (HostMetadata hostMetadata : decommissionCandidates) {
                String hostName = hostMetadata.getHostName();
                InstanceMetaData instanceMetaData = instanceMetadataRepository.findHostInStack(stack.getId(), hostName);
                if (!instanceMetaData.getAmbariServer()) {
                    if (i < adjustment) {
                        LOGGER.info("Host '{}' will be removed from Ambari cluster", hostName);
                        metadataToRemove.add(hostMetadata);
                        Set<String> components = ambariClient.getHostComponentsMap(hostName).keySet();
                        if (components.contains("NODEMANAGER")) {
                            int requestId = ambariClient.decommissionNodeManager(hostName);
                            decommissionRequests.put("NODEMANAGER_DECOMMISSION " + hostName, requestId);
                        }
                        if (components.contains("DATANODE")) {
                            int requestId = ambariClient.decommissionDataNode(hostName);
                            decommissionRequests.put("DATANODE_DECOMMISSION " + hostName, requestId);
                        }
                        if (components.contains("HBASE_REGIONSERVER")) {
                            int requestId = ambariClient.decommissionHBaseRegionServer(hostName);
                            decommissionRequests.put("HBASE_REGIONSERVER_DECOMMISSION " + hostName, requestId);
                        }
                        hostsWithComponents.put(hostName, new ArrayList<>(components));
                    } else {
                        break;
                    }
                    i++;
                }
            }
            waitForAmbariOperations(stack, ambariClient, decommissionRequests);
            waitForDataNodeDecommission(stack, ambariClient);
            stopHadoopComponents(stack, ambariClient, hostsWithComponents);
            deleteHostsFromAmbari(ambariClient, hostsWithComponents);
            restartHadoopServices(stack, ambariClient, true);
            cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
            cluster.getHostMetadata().removeAll(metadataToRemove);
            clusterRepository.save(cluster);
            Set<String> hostsRemoved = getRemovedHostNames(metadataToRemove);
            updateHostSuccessful(cluster, hostsRemoved, true);
        } catch (Exception e) {
            LOGGER.error(UNHANDLED_EXCEPTION_MSG, e);
            updateHostFailed(cluster, UNHANDLED_EXCEPTION_MSG, false);
        }
    }

    public boolean stopCluster(Stack stack) {
        return setClusterState(stack, true);
    }

    public boolean startCluster(Stack stack) {
        return setClusterState(stack, false);
    }

    private Set<String> getRemovedHostNames(Set<HostMetadata> metadataToRemove) {
        Set<String> hostsRemoved = new HashSet<>();
        for (HostMetadata hostMetadata : metadataToRemove) {
            hostsRemoved.add(hostMetadata.getHostName());
        }
        return hostsRemoved;
    }

    private void runSmokeTest(Stack stack, AmbariClient ambariClient) {
        int id = ambariClient.runMRServiceCheck();
        waitForAmbariOperations(stack, ambariClient, Collections.singletonMap("MR_SMOKE_TEST", id));
    }

    private PollingResult stopHadoopComponents(Stack stack, AmbariClient ambariClient, Map<String, List<String>> hostsWithComponents)
            throws HttpResponseException {
        Map<String, Integer> stopRequests = new HashMap<>();
        for (String host : hostsWithComponents.keySet()) {
            Map<String, Integer> resp = ambariClient.stopComponentsOnHost(host, hostsWithComponents.get(host));
            for (String component : resp.keySet()) {
                stopRequests.put(host + " " + component, resp.get(component));
            }
        }
        return waitForAmbariOperations(stack, ambariClient, stopRequests);
    }

    private void deleteHostsFromAmbari(AmbariClient ambariClient, Map<String, List<String>> hostsWithComponents) {
        for (String hostName : hostsWithComponents.keySet()) {
            ambariClient.deleteHostComponents(hostName, hostsWithComponents.get(hostName));
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
                requestId = ambariClient.stopAllServices();
            } else {
                requestId = ambariClient.startAllServices();
            }
        } catch (Exception e) {
            LOGGER.warn(String.format("Failed to %s Hadoop services", action), e);
            result = false;
        }
        if (requestId != -1) {
            LOGGER.info("Waiting for Hadoop services to {} on stack", action);
            operationsPollingService.pollWithTimeout(
                    ambariOperationsStatusCheckerTask,
                    new AmbariOperationsPollerObject(stack, ambariClient, singletonMap(action + " services", requestId)),
                    AmbariClusterConnector.POLLING_INTERVAL,
                    AmbariClusterConnector.MAX_ATTEMPTS_FOR_AMBARI_OPS);
        }
        return result;
    }

    private void addBlueprint(Stack stack, AmbariClient ambariClient, Blueprint blueprint) {
        MDCBuilder.buildMdcContext(stack);
        try {
            ambariClient.addBlueprint(blueprint.getBlueprintText(), hadoopConfigurationService.getConfiguration(stack));
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
                new AmbariHostsPollerObject(stack, ambariClient, stack.getNodeCount() * stack.getMultiplier()),
                POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_HOSTS);
    }

    private Map<String, List<String>> recommend(Stack stack, AmbariClient ambariClient, String blueprintName) throws InvalidHostGroupHostAssociation {
        MDCBuilder.buildMdcContext(stack);
        Map<String, List<String>> hostGroupMappings = new HashMap<>();
        LOGGER.info("Asking Ambari client to recommend host-hostGroup mapping [Ambari server address: {}]", stack.getAmbariIp());
        hostGroupMappings = ambariClient.recommendAssignments(blueprintName);
        LOGGER.info("recommended host-hostGroup mappings for stack: {}", hostGroupMappings);
        return hostGroupMappings;
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

    private void addHostMetadata(Cluster cluster, Map<String, String> hosts) {
        Set<HostMetadata> hostMetadata = new HashSet<>();
        for (Entry<String, String> host : hosts.entrySet()) {
            HostMetadata hostMetadataEntry = new HostMetadata();
            hostMetadataEntry.setHostName(host.getKey());
            hostMetadataEntry.setHostGroup(host.getValue());
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

    private Map<String, String> findHosts(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) {
        List<String> unregisteredHostNames = new ArrayList<>();
        Set<InstanceMetaData> unregisteredHosts = instanceMetadataRepository.findUnregisteredHostsInStack(stackId);
        for (InstanceMetaData instanceMetaData : unregisteredHosts) {
            unregisteredHostNames.add(instanceMetaData.getLongName());
        }
        Map<String, String> hosts = new HashMap<>();
        Integer scalingAdjustment = hostGroupAdjustment.getScalingAdjustment();
        String hostGroup = hostGroupAdjustment.getHostGroup();
        String statusReason = String.format("Adding '%s' new host(s) to the '%s' hostgroup.", scalingAdjustment, hostGroup);
        eventService.fireCloudbreakEvent(stackId, Status.UPDATE_IN_PROGRESS.name(), statusReason);
        for (int i = 0; i < scalingAdjustment; i++) {
            String host = unregisteredHostNames.get(0);
            hosts.put(host, hostGroup);
            unregisteredHostNames.remove(0);
        }
        return hosts;
    }

    private Map<String, Integer> installServices(Map<String, String> hosts, Stack stack, AmbariClient ambariClient) {
        Map<String, Integer> installRequests = new HashMap<>();
        for (Entry<String, String> host : hosts.entrySet()) {
            Map<String, Integer> hostInstallRequests = prepareHost(ambariClient, stack, host.getKey(), host.getValue());
            for (Entry<String, Integer> request : hostInstallRequests.entrySet()) {
                installRequests.put(String.format("%s-%s", host, request.getKey()), request.getValue());
            }
        }
        return installRequests;
    }

    private PollingResult waitForAmbariOperations(Stack stack, AmbariClient ambariClient, Map<String, Integer> operationRequests) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Waiting for Ambari operations to finish. [Operation requests: {}]", operationRequests);
        return waitForAmbariOperations(stack, ambariClient, ambariOperationsStatusCheckerTask, operationRequests);
    }

    private PollingResult waitForAmbariOperations(Stack stack, AmbariClient ambariClient, StatusCheckerTask task, Map<String, Integer> operationRequests) {
        return operationsPollingService.pollWithTimeout(
                task,
                new AmbariOperationsPollerObject(stack, ambariClient, operationRequests),
                POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_AMBARI_OPS);
    }

    private void waitForDataNodeDecommission(Stack stack, AmbariClient ambariClient) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Waiting for DataNodes to move the blocks to other nodes");
        waitForAmbariOperations(stack, ambariClient, dnDecommissionStatusCheckerTask, Collections.<String, Integer>emptyMap());
    }

    private Map<String, Integer> prepareHost(AmbariClient ambariClient, Stack stack, String host, String hostgroup) {
        String ambariIp = stack.getAmbariIp();
        MDCBuilder.buildMdcContext(stack);
        try {
            ambariClient.addHost(host);
            Map<String, Integer> installRequests = ambariClient.installComponentsToHost(host, stack.getCluster().getBlueprint().getBlueprintName(), hostgroup);
            LOGGER.info("Host added and service install requests are sent. [Ambari server: {}, host: '{}']", ambariIp, host);
            return installRequests;
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

    private void clusterCreateSuccess(Cluster cluster, long creationFinished, String ambariIp) {
        MDCBuilder.buildMdcContext(cluster);
        LOGGER.info("Publishing {} event", ReactorConfig.CLUSTER_CREATE_SUCCESS_EVENT);
        reactor.notify(ReactorConfig.CLUSTER_CREATE_SUCCESS_EVENT, Event.wrap(new ClusterCreationSuccess(cluster.getId(), creationFinished, ambariIp)));
    }

    private void clusterCreateFailed(Stack stack, Cluster cluster, String message) {
        MDCBuilder.buildMdcContext(cluster);
        LOGGER.info("Publishing {} event", ReactorConfig.CLUSTER_CREATE_FAILED_EVENT);
        reactor.notify(ReactorConfig.CLUSTER_CREATE_FAILED_EVENT, Event.wrap(new ClusterCreationFailure(stack.getId(), cluster.getId(), message)));
    }

    private void updateHostSuccessful(Cluster cluster, Set<String> hostNames, boolean decommission) {
        MDCBuilder.buildMdcContext(cluster);
        LOGGER.info("Publishing {} event", ReactorConfig.UPDATE_AMBARI_HOSTS_SUCCESS_EVENT);
        reactor.notify(ReactorConfig.UPDATE_AMBARI_HOSTS_SUCCESS_EVENT, Event.wrap(new UpdateAmbariHostsSuccess(cluster.getId(), hostNames, decommission)));
    }

    private void updateHostFailed(Cluster cluster, String message, boolean addingNodes) {
        MDCBuilder.buildMdcContext(cluster);
        LOGGER.info("Publishing {} event", ReactorConfig.UPDATE_AMBARI_HOSTS_FAILED_EVENT);
        reactor.notify(ReactorConfig.UPDATE_AMBARI_HOSTS_FAILED_EVENT, Event.wrap(new UpdateAmbariHostsFailure(cluster.getId(), message, addingNodes)));
    }

    public void checkClusterState(Stack stack) {
        MDCBuilder.buildMdcContext(stack.getCluster());
        try {
            if (clusterIsFailedState(stack)) {
                AmbariClient ambariClient = clientService.create(stack);
                Map<String, Map<String, String>> serviceComponentsMap = ambariClient.getServiceComponentsMap();
                boolean available = true;
                for (Entry<String, Map<String, String>> stringMapEntry : serviceComponentsMap.entrySet()) {
                    for (Entry<String, String> stringStringEntry : stringMapEntry.getValue().entrySet()) {
                        if (!"STARTED".equals(stringStringEntry.getValue()) && !stringStringEntry.getKey().endsWith("_CLIENT")) {
                            available = false;
                        }
                    }
                }
                if (available) {
                    Cluster cluster = clusterRepository.findById(stack.getCluster().getId());
                    cluster.setStatus(Status.AVAILABLE);
                    cluster.setStatusReason("The cluster and its services are available.");
                    clusterRepository.save(cluster);
                    stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE, "The cluster and its services are available.");
                }
            }
        } catch (Exception ex) {
            LOGGER.error("There was a problem with the ambari.");
        }

    }

    private boolean clusterIsFailedState(Stack stack) {
        return (Status.CREATE_FAILED.equals(stack.getCluster().getStatus()) || Status.START_FAILED.equals(stack.getCluster().getStatus())
                || Status.STOP_FAILED.equals(stack.getCluster().getStatus()) || Status.CREATE_IN_PROGRESS.equals(stack.getCluster().getStatus())
                || Status.UPDATE_IN_PROGRESS.equals(stack.getCluster().getStatus()))
                && (Status.CREATE_FAILED.equals(stack.getStatus()) || Status.START_FAILED.equals(stack.getStatus())
                || Status.STOP_FAILED.equals(stack.getStatus()) || Status.CREATE_IN_PROGRESS.equals(stack.getStatus())
                || Status.UPDATE_IN_PROGRESS.equals(stack.getStatus()));
    }
}
