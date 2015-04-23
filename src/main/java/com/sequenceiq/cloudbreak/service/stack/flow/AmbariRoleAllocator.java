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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
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

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private PollingService<ConsulContext> consulPollingService;

    @Autowired
    private ConsulHostCheckerTask consulHostCheckerTask;

    @Autowired
    private ConsulServiceCheckerTask consulServiceCheckerTask;

    public AmbariRoleAllocationComplete allocateRoles(Long stackId, Set<CoreInstanceMetaData> coreInstanceMetaData) {
        AmbariRoleAllocationComplete allocationComplete = null;

        Stack stack = stackRepository.findById(stackId);
        if (!stack.isMetadataReady()) {
            if (coreInstanceMetaData.size() != stack.getFullNodeCount().intValue()) {
                throw new WrongMetadataException(String.format(
                        "Size of the collected metadata set does not equal the node count of the stack. [metadata size=%s] [nodecount=%s]",
                        coreInstanceMetaData.size(), stack.getFullNodeCount()));
            }
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                Set<InstanceMetaData> instancesMetaData = prepareInstanceMetaData(coreInstanceMetaData, instanceGroup, InstanceStatus.UNREGISTERED);
                stackUpdater.updateStackMetaData(stackId, instancesMetaData, instanceGroup.getGroupName());
            }
            stack = stackUpdater.updateMetadataReady(stackId, true);
            Set<InstanceMetaData> allInstanceMetaData = stack.getRunningInstanceMetaData();
            Optional<String> publicAmbariAddress = updateAmbariInstanceMetadata(stack, allInstanceMetaData);
            if (publicAmbariAddress.isPresent()) {
                PollingResult pollingResult = waitForConsulAgents(stack, allInstanceMetaData, Collections.<InstanceMetaData>emptySet());
                if (isSuccess(pollingResult)) {
                    updateWithConsulData(allInstanceMetaData);
                    instanceMetaDataRepository.save(allInstanceMetaData);
                    allocationComplete = new AmbariRoleAllocationComplete(stack, publicAmbariAddress.orNull());
                }
            }
        } else {
            LOGGER.info("Metadata is already created, ignoring stack metadata update.");
            allocationComplete = new AmbariRoleAllocationComplete(stack, stack.getAmbariIp());
        }
        return allocationComplete;
    }

    public StackUpdateSuccess updateInstanceMetadata(Long stackId, Set<CoreInstanceMetaData> coreInstanceMetaData, String instanceGroupName) {
        StackUpdateSuccess stackUpdateSuccess = null;

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
        PollingResult pollingResult = waitForConsulAgents(modifiedStack, originalMetadata, instanceMetaData);
        if (isSuccess(pollingResult)) {
            updateWithConsulData(modifiedStack.getInstanceGroupByInstanceGroupName(instanceGroupName).getInstanceMetaData());
            stackUpdater.updateStackMetaData(stackId,
                    modifiedStack.getInstanceGroupByInstanceGroupName(instanceGroupName).getAllInstanceMetaData(), instanceGroupName);
            Set<String> instanceIds = new HashSet<>();
            for (InstanceMetaData metadataEntry : instanceMetaData) {
                instanceIds.add(metadataEntry.getInstanceId());
            }
            stackUpdateSuccess = new StackUpdateSuccess(stackId, false, instanceIds, instanceGroupName);
        }
        return stackUpdateSuccess;
    }

    private Optional<String> updateAmbariInstanceMetadata(Stack stack, Set<InstanceMetaData> instancesMetaData) {
        AmbariAddressReturnObject ambariAddress = getAmbariAddressFromConsul(stack, instancesMetaData);
        if (ambariAddress.getAddress().isPresent() && isSuccess(ambariAddress.getPollingResult().orNull())) {
            for (InstanceMetaData instanceMetaData : instancesMetaData) {
                if (instanceMetaData.getPrivateIp().equalsIgnoreCase(ambariAddress.getAddress().orNull())) {
                    instanceMetaData.setAmbariServer(true);
                    instanceMetaData.setInstanceStatus(InstanceStatus.REGISTERED);
                    return Optional.fromNullable(instanceMetaData.getPublicIp());
                }
            }
        } else if (isExited(ambariAddress.pollingResult.orNull())) {
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
            return successAmbariAddressReturnObject(getService(clients, AMBARI_SERVICE).get(0).getAddress());
        } else if (isExited(pollingResult)) {
            return exitedAmbariAddressReturnObject();
        } else {
            return failedAmbariAddressReturnObject();
        }
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

    private void updateWithConsulData(Set<InstanceMetaData> instancesMetaData) {
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