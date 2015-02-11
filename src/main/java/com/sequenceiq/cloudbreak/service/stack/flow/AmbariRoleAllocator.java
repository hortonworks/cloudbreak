package com.sequenceiq.cloudbreak.service.stack.flow;

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
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccStackUtil;
import com.sequenceiq.cloudbreak.service.stack.event.AmbariRoleAllocationComplete;
import com.sequenceiq.cloudbreak.service.stack.event.StackOperationFailure;
import com.sequenceiq.cloudbreak.service.stack.event.StackUpdateSuccess;

import reactor.core.Reactor;
import reactor.event.Event;

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
    private Reactor reactor;

    @Autowired
    private GccStackUtil gccStackUtil;

    @Autowired
    private PollingService<ConsulContext> consulPollingService;

    public void allocateRoles(Long stackId, Set<CoreInstanceMetaData> coreInstanceMetaData) {
        try {
            Stack stack = stackRepository.findById(stackId);
            MDCBuilder.buildMdcContext(stack);
            if (!stack.isMetadataReady()) {
                if (coreInstanceMetaData.size() != stack.getFullNodeCount()) {
                    throw new WrongMetadataException(String.format(
                            "Size of the collected metadata set does not equal the node count of the stack. [metadata size=%s] [nodecount=%s]",
                            coreInstanceMetaData.size(), stack.getFullNodeCount()));
                }
                for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                    Set<InstanceMetaData> instancesMetaData = prepareInstanceMetaData(coreInstanceMetaData, instanceGroup);
                    stackUpdater.updateStackMetaData(stackId, instancesMetaData, instanceGroup.getGroupName());
                }
                stack = stackUpdater.updateMetadataReady(stackId, true);
                Set<InstanceMetaData> allInstanceMetaData = stack.getRunningInstanceMetaData();
                String publicAmbariAddress = updateAmbariInstanceMetadata(stack, allInstanceMetaData);
                waitForConsulAgents(stack, allInstanceMetaData, Collections.<InstanceMetaData>emptySet());
                updateWithConsulData(allInstanceMetaData);
                instanceMetaDataRepository.save(allInstanceMetaData);
                LOGGER.info("Publishing {} event", ReactorConfig.AMBARI_ROLE_ALLOCATION_COMPLETE_EVENT);
                reactor.notify(ReactorConfig.AMBARI_ROLE_ALLOCATION_COMPLETE_EVENT, Event.wrap(new AmbariRoleAllocationComplete(stack,
                        publicAmbariAddress)));
            } else {
                LOGGER.info("Metadata is already created, ignoring '{}' event.", ReactorConfig.METADATA_SETUP_COMPLETE_EVENT);
            }
        } catch (WrongMetadataException e) {
            LOGGER.error(e.getMessage(), e);
            notifyStackCreateFailed(stackId, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unhandled exception occured while creating stack.", e);
            notifyStackCreateFailed(stackId, "Unhandled exception occured while creating stack.");
        }
    }

    public void updateInstanceMetadata(Long stackId, Set<CoreInstanceMetaData> coreInstanceMetaData, String hostGroup) {
        Stack one = stackRepository.findOneWithLists(stackId);
        InstanceGroup instanceGroup = one.getInstanceGroupByInstanceGroupName(hostGroup);
        MDCBuilder.buildMdcContext(one);
        try {
            Set<InstanceMetaData> originalMetadata = instanceGroup.getAllInstanceMetaData();
            Set<InstanceMetaData> instanceMetaData = prepareInstanceMetaData(coreInstanceMetaData, one.getInstanceGroupByInstanceGroupName(hostGroup));
            originalMetadata.addAll(instanceMetaData);
            Stack modifiedStack = stackUpdater.updateStackMetaData(stackId, originalMetadata, hostGroup);
            stackUpdater.updateMetadataReady(stackId, true);
            waitForConsulAgents(modifiedStack, originalMetadata, instanceMetaData);
            updateWithConsulData(modifiedStack.getInstanceGroupByInstanceGroupName(hostGroup).getInstanceMetaData());
            stackUpdater.updateStackMetaData(stackId, modifiedStack.getInstanceGroupByInstanceGroupName(hostGroup).getAllInstanceMetaData(), hostGroup);
            Set<String> instanceIds = new HashSet<>();
            for (InstanceMetaData metadataEntry : instanceMetaData) {
                instanceIds.add(metadataEntry.getInstanceId());
            }
            LOGGER.info("Publishing {} event.", ReactorConfig.STACK_UPDATE_SUCCESS_EVENT);
            reactor.notify(ReactorConfig.STACK_UPDATE_SUCCESS_EVENT, Event.wrap(new StackUpdateSuccess(stackId, false, instanceIds, hostGroup)));
        } catch (Exception e) {
            String errMessage = "Unhandled exception occurred while updating stack metadata.";
            LOGGER.error(errMessage, e);
            LOGGER.info("Publishing {} event.", ReactorConfig.STACK_UPDATE_FAILED_EVENT);
            reactor.notify(ReactorConfig.STACK_UPDATE_FAILED_EVENT, Event.wrap(new StackOperationFailure(stackId, errMessage)));
        }
    }

    private String updateAmbariInstanceMetadata(Stack stack, Set<InstanceMetaData> instancesMetaData) {
        String ambariAddress = getAmbariAddressFromConsul(stack, instancesMetaData);
        for (InstanceMetaData instanceMetaData : instancesMetaData) {
            if (instanceMetaData.getPrivateIp().equalsIgnoreCase(ambariAddress)) {
                instanceMetaData.setAmbariServer(true);
                instanceMetaData.setRemovable(false);
                return instanceMetaData.getPublicIp();
            }
        }
        throw new WrongMetadataException("Public IP of Ambari server cannot be null");
    }

    private String getAmbariAddressFromConsul(Stack stack, Set<InstanceMetaData> instancesMetaData) {
        List<ConsulClient> clients = createClients(instancesMetaData);
        consulPollingService.pollWithTimeout(
                new ConsulServiceCheckerTask(),
                new ConsulContext(stack, clients, Arrays.asList(AMBARI_SERVICE)),
                POLLING_INTERVAL,
                MAX_POLLING_ATTEMPTS);
        return getService(clients, AMBARI_SERVICE).get(0).getAddress();
    }

    private void waitForConsulAgents(Stack stack, Set<InstanceMetaData> originalMetaData, Set<InstanceMetaData> instancesMetaData) {
        Set<InstanceMetaData> copy = new HashSet<>(originalMetaData);
        copy.removeAll(instancesMetaData);
        List<ConsulClient> clients = createClients(copy);
        List<String> privateIps = new ArrayList<>(instancesMetaData.size());
        for (InstanceMetaData instance : instancesMetaData) {
            privateIps.add(instance.getPrivateIp());
        }
        consulPollingService.pollWithTimeout(
                new ConsulHostCheckerTask(),
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

    private Set<InstanceMetaData> prepareInstanceMetaData(Set<CoreInstanceMetaData> coreInstanceMetaData, InstanceGroup instanceGroup) {
        Set<InstanceMetaData> instanceMetaData = new HashSet<>();
        for (CoreInstanceMetaData coreInstanceMetaDataEntry : coreInstanceMetaData) {
            if (coreInstanceMetaDataEntry.getInstanceGroup().getGroupName().equals(instanceGroup.getGroupName())) {
                long timeInMillis = Calendar.getInstance().getTimeInMillis();
                InstanceMetaData instanceMetaDataEntry = new InstanceMetaData();
                instanceMetaDataEntry.setPrivateIp(coreInstanceMetaDataEntry.getPrivateIp());
                instanceMetaDataEntry.setInstanceGroup(coreInstanceMetaDataEntry.getInstanceGroup());
                instanceMetaDataEntry.setPublicIp(coreInstanceMetaDataEntry.getPublicDns());
                instanceMetaDataEntry.setInstanceId(coreInstanceMetaDataEntry.getInstanceId());
                instanceMetaDataEntry.setVolumeCount(coreInstanceMetaDataEntry.getVolumeCount());
                instanceMetaDataEntry.setLongName(coreInstanceMetaDataEntry.getLongName());
                instanceMetaDataEntry.setDockerSubnet(String.format("172.18.%s.1", coreInstanceMetaDataEntry.getPrivateIp().split("\\.")[LAST]));
                instanceMetaDataEntry.setContainerCount(coreInstanceMetaDataEntry.getContainerCount());
                instanceMetaDataEntry.setAmbariServer(Boolean.FALSE);
                instanceMetaDataEntry.setRemovable(true);
                instanceMetaDataEntry.setStartDate(timeInMillis);
                instanceMetaData.add(instanceMetaDataEntry);
            }
        }
        return instanceMetaData;
    }

    private void notifyStackCreateFailed(Long stackId, String cause) {
        MDCBuilder.buildMdcContext();
        LOGGER.info("Publishing {} event ", ReactorConfig.STACK_CREATE_FAILED_EVENT, stackId);
        StackOperationFailure stackCreationFailure = new StackOperationFailure(stackId, cause);
        reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(stackCreationFailure));
    }

}