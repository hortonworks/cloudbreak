package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils.createClients;
import static com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils.getAliveMembers;
import static com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils.getService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
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
import com.sequenceiq.cloudbreak.service.stack.event.StackUpdateSuccess;

@Service
public class AmbariRoleAllocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariRoleAllocator.class);
    private static final int LAST = 3;
    private static final String AMBARI_SERVICE = "ambari-8080";
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
    private ConsulServiceCheckerTask consulServiceCheckerTask;

    @Autowired
    private PollingService<AmbariHosts> hostsPollingService;

    @Autowired
    private AmbariHostsStatusCheckerTask ambariHostsStatusCheckerTask;

    @Autowired
    private CloudbreakEventService eventService;


    public AmbariRoleAllocationComplete allocateRoles(Long stackId, Set<CoreInstanceMetaData> coreInstanceMetaData) {
        AmbariRoleAllocationComplete allocationComplete = null;

        Stack stack = stackRepository.findById(stackId);
        MDCBuilder.buildMdcContext(stack);
        Set<InstanceMetaData> allInstanceMetaData = stack.getAllInstanceMetaData();
        PollingResult pollingResult = waitForConsulAgents(stack, allInstanceMetaData, Collections.<InstanceMetaData>emptySet());
        if (isSuccess(pollingResult)) {
            updateWithConsulData(allInstanceMetaData);
            instanceMetaDataRepository.save(allInstanceMetaData);
            allocationComplete = new AmbariRoleAllocationComplete(stack, stack.getAmbariIp());
        }
        return allocationComplete;
    }

    public AmbariRoleAllocationComplete allocateRoles(Long stackId) {
        AmbariRoleAllocationComplete allocationComplete = null;

        Stack stack = stackRepository.findById(stackId);
        MDCBuilder.buildMdcContext(stack);
        Set<InstanceMetaData> allInstanceMetaData = stack.getAllInstanceMetaData();
        PollingResult pollingResult = waitForConsulAgents(stack, allInstanceMetaData, Collections.<InstanceMetaData>emptySet());
        if (isSuccess(pollingResult)) {
            updateWithConsulData(allInstanceMetaData);
            instanceMetaDataRepository.save(allInstanceMetaData);
            allocationComplete = new AmbariRoleAllocationComplete(stack, stack.getAmbariIp());
        }
        return allocationComplete;
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

    public StackUpdateSuccess updateInstanceMetadata(Long stackId, Set<CoreInstanceMetaData> coreInstanceMetaData, String instanceGroupName) {
        Stack one = stackRepository.findOneWithLists(stackId);
        InstanceGroup instanceGroup = one.getInstanceGroupByInstanceGroupName(instanceGroupName);
        Set<InstanceMetaData> originalMetadata = instanceGroup.getAllInstanceMetaData();
        Set<InstanceMetaData> instanceMetaData = prepareInstanceMetaData(
                coreInstanceMetaData,
                one.getInstanceGroupByInstanceGroupName(instanceGroupName),
                InstanceStatus.UNREGISTERED);
        originalMetadata.addAll(instanceMetaData);
        Stack modifiedStack = stackUpdater.updateStackMetaData(stackId, originalMetadata, instanceGroupName);
        stackUpdater.updateMetadataReady(stackId, true);
        Set<InstanceMetaData> newInstanceMetadata = modifiedStack.getInstanceGroupByInstanceGroupName(instanceGroupName).getInstanceMetaData();
        instanceMetaDataRepository.save(updateWithNewNodesConsulData(newInstanceMetadata));
        Set<String> instanceIds = new HashSet<>();
        for (InstanceMetaData metadataEntry : instanceMetaData) {
            instanceIds.add(metadataEntry.getInstanceId());
        }
        return new StackUpdateSuccess(stackId, false, instanceIds, instanceGroupName);
    }

    private PollingResult waitForHosts(Stack stack, AmbariClient ambariClient) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Waiting for hosts to connect.[Ambari server address: {}]", stack.getAmbariIp());
        return hostsPollingService.pollWithTimeout(
                ambariHostsStatusCheckerTask,
                new AmbariHosts(stack, ambariClient, stack.getFullNodeCountWithoutDecommissionedNodes() - stack.getGateWayNodeCount()),
                POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_HOSTS);
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

    public Optional<CoreInstanceMetaData> getGateWayCoreInstanceMetaData(Set<CoreInstanceMetaData> coreInstanceMetaData) {
        for (CoreInstanceMetaData instanceMetaData : coreInstanceMetaData) {
            if (InstanceGroupType.isGateway(instanceMetaData.getInstanceGroup().getInstanceGroupType())) {
                return Optional.of(instanceMetaData);
            }
        }
        return Optional.absent();
    }

    private Optional<String> updateAmbariInstanceMetadata(Set<InstanceMetaData> instancesMetaData, Optional<CoreInstanceMetaData> gatewayMetadata) {
        if (gatewayMetadata.isPresent()) {
            for (InstanceMetaData instanceMetaData : instancesMetaData) {
                if (instanceMetaData.getPrivateIp().equalsIgnoreCase(gatewayMetadata.get().getPrivateIp())) {
                    instanceMetaData.setAmbariServer(true);
                    instanceMetaData.setInstanceStatus(InstanceStatus.REGISTERED);
                    return Optional.fromNullable(instanceMetaData.getPublicIp());
                }
            }
        } else {
            return Optional.absent();
        }
        throw new WrongMetadataException("Public IP of Ambari server cannot be null");
    }

    private AmbariAddressReturnObject getAmbariAddressFromConsul(Stack stack, Set<InstanceMetaData> instancesMetaData) {
        List<ConsulClient> clients = createClients(instancesMetaData);
        PollingResult pollingResult = consulPollingService.pollWithTimeout(
                consulServiceCheckerTask,
                new ConsulContext(stack, clients, Arrays.asList(AMBARI_SERVICE)),
                POLLING_INTERVAL,
                MAX_POLLING_ATTEMPTS);
        if (isSuccess(pollingResult)) {
            return successAmbariAddressReturnObject(getAmbariAddress(clients));
        } else if (isExited(pollingResult)) {
            return exitedAmbariAddressReturnObject();
        } else {
            return failedAmbariAddressReturnObject();
        }
    }

    @VisibleForTesting
    protected String getAmbariAddress(List<ConsulClient> clients) {
        return getService(clients, AMBARI_SERVICE).get(0).getAddress();
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

    private Set<InstanceMetaData> updateWithNewNodesConsulData(Set<InstanceMetaData> instancesMetaData) {
        for (InstanceMetaData instanceMetaData : instancesMetaData) {
            if (!instanceMetaData.getLongName().endsWith(ConsulUtils.CONSUL_DOMAIN)
                    && InstanceStatus.UNREGISTERED.equals(instanceMetaData.getInstanceStatus())) {
                instanceMetaData.setConsulServer(false);
                instanceMetaData.setLongName(instanceMetaData.getInstanceId() + ConsulUtils.CONSUL_DOMAIN);
            }
        }
        return instancesMetaData;
    }

    private Set<String> getConsulServers(List<ConsulClient> clients) {
        List<CatalogService> services = getService(clients, CONSUL_SERVICE);
        Set<String> privateIps = new HashSet<>();
        for (CatalogService service : services) {
            privateIps.add(service.getAddress());
        }
        return privateIps;
    }

    private Set<InstanceMetaData> prepareInstanceMetaData(Set<CoreInstanceMetaData> coreInstanceMetaData, InstanceGroup instanceGroup, InstanceStatus
            status) {
        Set<InstanceMetaData> instanceMetaData = new HashSet<>();
        for (CoreInstanceMetaData coreInstanceMetaDataEntry : coreInstanceMetaData) {
            if (coreInstanceMetaDataEntry.getInstanceGroup().getGroupName().equals(instanceGroup.getGroupName())) {
                long timeInMillis = Calendar.getInstance().getTimeInMillis();
                InstanceMetaData instanceMetaDataEntry = new InstanceMetaData();
                instanceMetaDataEntry.setPrivateIp(coreInstanceMetaDataEntry.getPrivateIp());
                instanceMetaDataEntry.setInstanceGroup(coreInstanceMetaDataEntry.getInstanceGroup());
                instanceMetaDataEntry.setPublicIp(coreInstanceMetaDataEntry.getPublicIp());
                instanceMetaDataEntry.setInstanceId(coreInstanceMetaDataEntry.getInstanceId());
                instanceMetaDataEntry.setVolumeCount(coreInstanceMetaDataEntry.getVolumeCount());
                instanceMetaDataEntry.setLongName(coreInstanceMetaDataEntry.getLongName());
                instanceMetaDataEntry.setDockerSubnet(String.format("172.18.%s.1", coreInstanceMetaDataEntry.getPrivateIp().split("\\.")[LAST]));
                instanceMetaDataEntry.setContainerCount(coreInstanceMetaDataEntry.getContainerCount());
                instanceMetaDataEntry.setAmbariServer(Boolean.FALSE);
                instanceMetaDataEntry.setInstanceStatus(status);
                instanceMetaDataEntry.setStartDate(timeInMillis);
                instanceMetaData.add(instanceMetaDataEntry);
            }
        }
        return instanceMetaData;
    }

    public AmbariAddressReturnObject successAmbariAddressReturnObject(String address) {
        return new AmbariAddressReturnObject(Optional.fromNullable(address), Optional.fromNullable(PollingResult.SUCCESS));
    }

    public AmbariAddressReturnObject exitedAmbariAddressReturnObject() {
        return new AmbariAddressReturnObject(Optional.<String>absent(), Optional.fromNullable(PollingResult.EXIT));
    }

    public AmbariAddressReturnObject failedAmbariAddressReturnObject() {
        return new AmbariAddressReturnObject(Optional.<String>absent(), Optional.<PollingResult>absent());
    }

    public class AmbariAddressReturnObject {
        private final Optional<String> address;
        private final Optional<PollingResult> pollingResult;

        private AmbariAddressReturnObject(Optional<String> address, Optional<PollingResult> pollingResult) {
            this.address = address;
            this.pollingResult = pollingResult;
        }

        public Optional<String> getAddress() {
            return address;
        }

        public Optional<PollingResult> getPollingResult() {
            return pollingResult;
        }
    }

}