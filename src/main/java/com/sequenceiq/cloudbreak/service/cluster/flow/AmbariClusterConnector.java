package com.sequenceiq.cloudbreak.service.cluster.flow;

import groovyx.net.http.HttpResponseException;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.Reactor;
import reactor.event.Event;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.InvalidHostGroupHostAssociation;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClusterService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariHostsUnavailableException;
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.service.cluster.HadoopConfiguration;
import com.sequenceiq.cloudbreak.service.cluster.event.AddAmbariHostsFailure;
import com.sequenceiq.cloudbreak.service.cluster.event.AddAmbariHostsSuccess;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterCreationFailure;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterCreationSuccess;
import com.sequenceiq.cloudbreak.service.stack.connector.HadoopConfigurationProvider;

@Service
public class AmbariClusterConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterConnector.class);

    private static final int POLLING_INTERVAL = 5000;
    private static final int MAX_ATTEMPTS_FOR_AMBARI_OPS = -1;
    private static final int MAX_ATTEMPTS_FOR_HOSTS = 240;

    private static final String UNHANDLED_EXCEPTION_MSG = "Unhandled exception occurred while installing Ambari services.";

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private HostMetadataRepository hostMetadataRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private Reactor reactor;

    @Resource
    private Map<CloudPlatform, HadoopConfigurationProvider> hadoopConfigurationProviders;

    @Autowired
    private PollingService<AmbariOperations> operationsPollingService;

    @Autowired
    private PollingService<AmbariHosts> hostsPollingService;

    public void installAmbariCluster(Stack stack) {
        Cluster cluster = stack.getCluster();
        try {
            LOGGER.info("Starting Ambari cluster installation for stack '{}' [Ambari server address: {}]", stack.getId(), stack.getAmbariIp());
            stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS);
            cluster.setCreationStarted(new Date().getTime());
            cluster = clusterRepository.save(cluster);
            Blueprint blueprint = cluster.getBlueprint();
            AmbariClient ambariClient = createAmbariClient(stack.getAmbariIp());

            addBlueprint(stack, ambariClient, blueprint);
            Map<String, List<String>> hostGroupMappings = recommend(stack, ambariClient, blueprint.getBlueprintName());
            saveHostMetadata(cluster, hostGroupMappings);
            ambariClient.createCluster(cluster.getName(), blueprint.getBlueprintName(), hostGroupMappings);
            waitForClusterInstall(stack, ambariClient);
            clusterCreateSuccess(cluster, new Date().getTime(), stack.getAmbariIp());
        } catch (AmbariHostsUnavailableException | AmbariOperationFailedException | InvalidHostGroupHostAssociation e) {
            LOGGER.error(e.getMessage(), e);
            clusterCreateFailed(cluster, e.getMessage());
        } catch (Exception e) {
            LOGGER.error(UNHANDLED_EXCEPTION_MSG, e);
            clusterCreateFailed(cluster, UNHANDLED_EXCEPTION_MSG);
        }
    }

    public void installAmbariNode(Long stackId, Set<HostGroupAdjustmentJson> hostGroupAdjustments) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        try {
            stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS);
            AmbariClient ambariClient = createAmbariClient(stack.getAmbariIp());
            waitForHosts(stack, ambariClient);
            Map<String, String> hosts = findHosts(hostGroupAdjustments, ambariClient);
            addHostMetadata(cluster, hosts);
            Map<String, Integer> installRequests = installServices(hosts, stack, ambariClient);
            waitForServiceInstalls(stack, ambariClient, installRequests);
            ambariClient.startAllServices();
            ambariClient.restartServiceComponents("NAGIOS", Arrays.asList("NAGIOS_SERVER"));
            addHostSuccessful(cluster, hosts.keySet());
        } catch (AmbariHostsUnavailableException | AmbariOperationFailedException e) {
            LOGGER.error(e.getMessage(), e);
            addHostFailed(cluster, e.getMessage());
        } catch (Exception e) {
            LOGGER.error(UNHANDLED_EXCEPTION_MSG, e);
            addHostFailed(cluster, UNHANDLED_EXCEPTION_MSG);
        }
    }

    public void decommisionAmbariNodes(Long stackId, Set<HostGroupAdjustmentJson> hosts) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = stack.getCluster();
        try {
            AmbariClient ambariClient = createAmbariClient(stack.getAmbariIp());
            Set<HostMetadata> metadataToRemove = new HashSet<>();
            for (HostGroupAdjustmentJson hostGroupAdjustment : hosts) {
                Set<HostMetadata> hostsInHostGroup = hostMetadataRepository.findHostsInHostgroup(hostGroupAdjustment.getHostGroup(), cluster.getId());
                int i = 0;
                for (HostMetadata hostMetadata : hostsInHostGroup) {
                    if (i < -1 * hostGroupAdjustment.getScalingAdjustment()) {
                        metadataToRemove.add(hostMetadata);
                        ambariClient.removeHost(hostMetadata.getHostName());
                    } else {
                        break;
                    }
                    i++;
                }
            }
            cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
            cluster.getHostMetadata().removeAll(metadataToRemove);
            clusterRepository.save(cluster);
        } catch (Exception e) {
            LOGGER.error(UNHANDLED_EXCEPTION_MSG, e);
            addHostFailed(cluster, UNHANDLED_EXCEPTION_MSG);
        }
    }

    @VisibleForTesting
    protected AmbariClient createAmbariClient(String ambariIp) {
        return new AmbariClient(ambariIp, AmbariClusterService.PORT);
    }

    private void addBlueprint(Stack stack, AmbariClient ambariClient, Blueprint blueprint) {
        try {
            ambariClient.addBlueprint(blueprint.getBlueprintText(), getExtendConfig(stack));
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

    private Map<String, Map<String, String>> getExtendConfig(Stack stack) {
        Map<String, Map<String, String>> extendConfig = new HashMap<>();
        HadoopConfigurationProvider hadoopConfigurationProvider = hadoopConfigurationProviders.get(stack.getTemplate().cloudPlatform());
        extendConfig.put(HadoopConfiguration.YARN_SITE.getKey(), hadoopConfigurationProvider.getYarnSiteConfigs(stack));
        extendConfig.put(HadoopConfiguration.HDFS_SITE.getKey(), hadoopConfigurationProvider.getHdfsSiteConfigs(stack));
        return extendConfig;
    }

    private void waitForHosts(Stack stack, AmbariClient ambariClient) {
        LOGGER.info("Waiting for hosts to connect. [Stack: {}, Ambari server address: {}]", stack.getId(), stack.getAmbariIp());
        hostsPollingService.pollWithTimeout(
                new AmbariHostsStatusCheckerTask(),
                new AmbariHosts(stack.getId(), ambariClient, stack.getNodeCount()),
                POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_HOSTS);
    }

    private Map<String, List<String>> recommend(Stack stack, AmbariClient ambariClient, String blueprintName) throws InvalidHostGroupHostAssociation {
        waitForHosts(stack, ambariClient);
        LOGGER.info("Asking Ambari client to recommend host-hostGroup mapping [Stack: {}, Ambari server address: {}]", stack.getId(), stack.getAmbariIp());
        Map<String, List<String>> hostGroupMappings = ambariClient.recommendAssignments(blueprintName);
        LOGGER.info("recommended host-hostGroup mappings for stack {}: {}", stack.getId(), hostGroupMappings);
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

    private void waitForClusterInstall(Stack stack, AmbariClient ambariClient) {
        Map<String, Integer> clusterInstallRequest = new HashMap<>();
        clusterInstallRequest.put("CLUSTER_INSTALL", 1);
        waitForServiceInstalls(stack, ambariClient, clusterInstallRequest);
    }

    private Map<String, String> findHosts(Set<HostGroupAdjustmentJson> hostGroupAdjustments, AmbariClient ambariClient) {
        List<String> unregisteredHostNames = ambariClient.getUnregisteredHostNames();
        Map<String, String> hosts = new HashMap<>();
        for (HostGroupAdjustmentJson entry : hostGroupAdjustments) {
            for (int i = 0; i < entry.getScalingAdjustment(); i++) {
                String host = unregisteredHostNames.get(0);
                hosts.put(host, entry.getHostGroup());
                unregisteredHostNames.remove(0);
            }
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

    private void waitForServiceInstalls(Stack stack, AmbariClient ambariClient, Map<String, Integer> installRequests) {
        LOGGER.info("Waiting for Ambari services to finish installation. [Stack: '{}', Install requests: {}]", stack.getId(), installRequests);
        operationsPollingService.pollWithTimeout(
                new AmbariOperationsStatusCheckerTask(),
                new AmbariOperations(stack.getId(), ambariClient, installRequests),
                POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_AMBARI_OPS);
    }

    private Map<String, Integer> prepareHost(AmbariClient ambariClient, Stack stack, String host, String hostgroup) {
        String ambariIp = stack.getAmbariIp();
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
        LOGGER.info("Publishing {} event [ClusterId: '{}']", ReactorConfig.CLUSTER_CREATE_SUCCESS_EVENT, cluster.getId());
        reactor.notify(ReactorConfig.CLUSTER_CREATE_SUCCESS_EVENT, Event.wrap(new ClusterCreationSuccess(cluster.getId(), creationFinished, ambariIp)));
    }

    private void clusterCreateFailed(Cluster cluster, String message) {
        LOGGER.info("Publishing {} event [ClusterId: '{}']", ReactorConfig.CLUSTER_CREATE_FAILED_EVENT, cluster.getId());
        reactor.notify(ReactorConfig.CLUSTER_CREATE_FAILED_EVENT, Event.wrap(new ClusterCreationFailure(cluster.getId(), message)));
    }

    private void addHostSuccessful(Cluster cluster, Set<String> hostNames) {
        LOGGER.info("Publishing {} event [ClusterId: '{}']", ReactorConfig.ADD_AMBARI_HOSTS_SUCCESS_EVENT, cluster.getId());
        reactor.notify(ReactorConfig.ADD_AMBARI_HOSTS_SUCCESS_EVENT, Event.wrap(new AddAmbariHostsSuccess(cluster.getId(), hostNames)));
    }

    private void addHostFailed(Cluster cluster, String message) {
        LOGGER.info("Publishing {} event [ClusterId: '{}']", ReactorConfig.ADD_AMBARI_HOSTS_FAILED_EVENT, cluster.getId());
        reactor.notify(ReactorConfig.ADD_AMBARI_HOSTS_FAILED_EVENT, Event.wrap(new AddAmbariHostsFailure(cluster.getId(), message)));
    }
}
