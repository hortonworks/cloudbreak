package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils.createClients;
import static com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils.getAliveMembers;
import static com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils.getService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.ScalingType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariHosts;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariHostsStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.event.AmbariRoleAllocationComplete;

@Service
public class AmbariRoleAllocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariRoleAllocator.class);
    private static final int LAST = 3;
    private static final String CONSUL_SERVICE = "consul";
    private static final int POLLING_INTERVAL = 5000;
    private static final int MAX_POLLING_ATTEMPTS = 100;
    private static final int MAX_ATTEMPTS_FOR_HOSTS = 240;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private HostGroupRepository hostGroupRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private PollingService<ConsulContext> consulPollingService;

    @Autowired
    private ConsulHostCheckerTask consulHostCheckerTask;

    @Autowired
    private PollingService<AmbariHosts> hostsPollingService;

    @Autowired
    private AmbariHostsStatusCheckerTask ambariHostsStatusCheckerTask;

    @Autowired
    private CloudbreakEventService eventService;

    public AmbariRoleAllocationComplete allocateRoles(Long stackId) {
        AmbariRoleAllocationComplete allocationComplete = null;

        Stack stack = stackRepository.findById(stackId);
        MDCBuilder.buildMdcContext(stack);
        Set<InstanceMetaData> allInstanceMetaData = stack.getAllInstanceMetaData();
        PollingResult pollingResult = waitForConsulAgents(stack, allInstanceMetaData, Collections.<InstanceMetaData>emptySet());
        if (!isSuccess(pollingResult)) {
            throw new WrongMetadataException("Connecting to consul hosts is interrupted.");
        }
        updateWithConsulData(allInstanceMetaData);
        instanceMetaDataRepository.save(allInstanceMetaData);
        return new AmbariRoleAllocationComplete(stack, stack.getAmbariIp());
    }

    public ClusterScalingContext updateNewInstanceMetadata(Long stackId, HostGroupAdjustmentJson adjustment, Set<String> instanceIds, ScalingType scalingType) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        MDCBuilder.buildMdcContext(cluster);
        stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS, "Adding new host(s) to the cluster.");
        HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), adjustment.getHostGroup());
        List<String> hosts = findFreeHosts(stack.getId(), hostGroup, adjustment.getScalingAdjustment());
        List<HostMetadata> hostMetadata = addHostMetadata(cluster, hosts, adjustment);
        ClusterScalingContext clusterScalingContext =
                new ClusterScalingContext(stackId, stack.cloudPlatform(), adjustment, instanceIds, hostMetadata, scalingType);
        return clusterScalingContext;
    }

    private List<String> findFreeHosts(Long stackId, HostGroup hostGroup, int scalingAdjustment) {
        Set<InstanceMetaData> unregisteredHosts = instanceMetaDataRepository.findUnregisteredHostsInInstanceGroup(hostGroup.getInstanceGroup().getId());
        Set<InstanceMetaData> instances = FluentIterable.from(unregisteredHosts).limit(scalingAdjustment).toSet();
        String statusReason = String.format("Adding '%s' new host(s) to the '%s' host group.", scalingAdjustment, hostGroup.getName());
        eventService.fireCloudbreakInstanceGroupEvent(stackId, Status.UPDATE_IN_PROGRESS.name(), statusReason, hostGroup.getName());
        return getHostNames(instances);
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

    private List<HostMetadata> addHostMetadata(Cluster cluster, List<String> hosts, HostGroupAdjustmentJson hostGroupAdjustment) {
        List<HostMetadata> hostMetadata = new ArrayList<>();
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

    private PollingResult waitForConsulAgents(Stack stack, Set<InstanceMetaData> originalMetaData, Set<InstanceMetaData> instancesMetaData) {
        Set<InstanceMetaData> copy = new HashSet<>(originalMetaData);
        copy.removeAll(instancesMetaData);
        List<ConsulClient> clients = createClients(copy);
        List<String> privateIps = new ArrayList<>();
        if (instancesMetaData.isEmpty()) {
            for (InstanceMetaData instance : originalMetaData) {
                privateIps.add(instance.getPrivateIp());
            }
        } else {
            for (InstanceMetaData instance : instancesMetaData) {
                privateIps.add(instance.getPrivateIp());
            }
        }
        return consulPollingService.pollWithTimeout(
                consulHostCheckerTask,
                new ConsulContext(stack, clients, privateIps),
                POLLING_INTERVAL,
                MAX_POLLING_ATTEMPTS);
    }

    @VisibleForTesting
    protected void updateWithConsulData(Set<InstanceMetaData> instancesMetaData) {
        List<ConsulClient> clients = createClients(instancesMetaData);
        Map<String, String> members = getAliveMembers(clients);
        Set<String> consulServers = getConsulServers(clients);
        for (InstanceMetaData instanceMetaData : instancesMetaData) {
            String privateIp = instanceMetaData.getPrivateIp();
            String address = members.get(privateIp);
            if (!instanceMetaData.getLongName().endsWith(ConsulUtils.CONSUL_DOMAIN)) {
                if (consulServers.contains(privateIp)) {
                    instanceMetaData.setConsulServer(true);
                } else {
                    instanceMetaData.setConsulServer(false);
                }
                instanceMetaData.setLongName(address + ConsulUtils.CONSUL_DOMAIN);
            }
        }
    }

    private Set<String> getConsulServers(List<ConsulClient> clients) {
        List<CatalogService> services = getService(clients, CONSUL_SERVICE);
        Set<String> privateIps = new HashSet<>();
        for (CatalogService service : services) {
            privateIps.add(service.getAddress());
        }
        return privateIps;
    }

}