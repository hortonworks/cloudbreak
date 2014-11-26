package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
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
    private static final int POLLING_INTERVAL = 5000;
    private static final int MAX_POLLING_ATTEMPTS = 10000;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private Reactor reactor;

    @Autowired
    private GccStackUtil gccStackUtil;

    @Autowired
    private PollingService<ConsulService> consulPollingService;

    public void allocateRoles(Long stackId, Set<CoreInstanceMetaData> coreInstanceMetaData) {
        try {
            Stack stack = stackRepository.findById(stackId);
            MDCBuilder.buildMdcContext(stack);
            if (!stack.isMetadataReady()) {
                if (coreInstanceMetaData.size() != stack.getNodeCount()) {
                    throw new WrongMetadataException(String.format(
                            "Size of the collected metadata set does not equal the node count of the stack. [metadata size=%s] [nodecount=%s]",
                            coreInstanceMetaData.size(), stack.getNodeCount()));
                }
                Set<InstanceMetaData> instancesMetaData = prepareInstanceMetaData(stack, coreInstanceMetaData);
                stackUpdater.updateStackMetaData(stackId, instancesMetaData);
                stackUpdater.updateMetadataReady(stackId, true);
                String privateAmbariAddress = getAmbariAddressFromConsul(instancesMetaData);
                String publicAmbariAddress = updateAmbariInstanceMetadata(stackId, privateAmbariAddress, instancesMetaData);
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

    public void updateInstanceMetadata(Long stackId, Set<CoreInstanceMetaData> coreInstanceMetaData) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        MDCBuilder.buildMdcContext(stack);
        try {
            Set<InstanceMetaData> originalMetadata = stack.getInstanceMetaData();
            Set<InstanceMetaData> instanceMetaData = prepareInstanceMetaData(stack, coreInstanceMetaData, stack.getInstanceMetaData().size() + 1);
            originalMetadata.addAll(instanceMetaData);
            stackUpdater.updateStackMetaData(stackId, originalMetadata);
            stackUpdater.updateMetadataReady(stackId, true);
            Set<String> instanceIds = new HashSet<>();
            for (InstanceMetaData metadataEntry : instanceMetaData) {
                instanceIds.add(metadataEntry.getInstanceId());
            }
            LOGGER.info("Publishing {} event.", ReactorConfig.STACK_UPDATE_SUCCESS_EVENT);
            reactor.notify(ReactorConfig.STACK_UPDATE_SUCCESS_EVENT, Event.wrap(new StackUpdateSuccess(stackId, false, instanceIds)));
        } catch (Exception e) {
            String errMessage = "Unhandled exception occurred while updating stack metadata.";
            LOGGER.error(errMessage, e);
            LOGGER.info("Publishing {} event.", ReactorConfig.STACK_UPDATE_FAILED_EVENT);
            reactor.notify(ReactorConfig.STACK_UPDATE_FAILED_EVENT, Event.wrap(new StackOperationFailure(stackId, errMessage)));
        }
    }

    private String getAmbariAddressFromConsul(Set<InstanceMetaData> instancesMetaData) {
        InstanceMetaData metaData = instancesMetaData.iterator().next();
        ConsulClient consulClient = new ConsulClient(metaData.getPublicIp());
        consulPollingService.pollWithTimeout(
                new ConsulServiceCheckerTask(),
                new ConsulService(consulClient, AMBARI_SERVICE),
                POLLING_INTERVAL,
                MAX_POLLING_ATTEMPTS);
        List<CatalogService> catalog = consulClient.getCatalogService(AMBARI_SERVICE, QueryParams.DEFAULT).getValue();
        return catalog.get(0).getAddress();
    }

    private String updateAmbariInstanceMetadata(long stackId, String ambariAddress, Set<InstanceMetaData> instancesMetaData) {
        for (InstanceMetaData instanceMetaData : instancesMetaData) {
            if (instanceMetaData.getPrivateIp().equalsIgnoreCase(ambariAddress)) {
                instanceMetaData.setAmbariServer(true);
                instanceMetaData.setRemovable(false);
                stackUpdater.updateStackMetaData(stackId, instancesMetaData);
                return instanceMetaData.getPublicIp();
            }
        }
        throw new WrongMetadataException(String.format("Public IP of Ambari server cannot be null [stack: '%s']", stackId));
    }

    private Set<InstanceMetaData> prepareInstanceMetaData(Stack stack, Set<CoreInstanceMetaData> coreInstanceMetaData) {
        return prepareInstanceMetaData(stack, coreInstanceMetaData, 0);
    }

    private Set<InstanceMetaData> prepareInstanceMetaData(Stack stack, Set<CoreInstanceMetaData> coreInstanceMetaData, int startIndex) {
        Set<InstanceMetaData> instanceMetaData = new HashSet<>();
        for (CoreInstanceMetaData coreInstanceMetaDataEntry : coreInstanceMetaData) {
            InstanceMetaData instanceMetaDataEntry = new InstanceMetaData();
            instanceMetaDataEntry.setPrivateIp(coreInstanceMetaDataEntry.getPrivateIp());
            instanceMetaDataEntry.setPublicIp(coreInstanceMetaDataEntry.getPublicDns());
            instanceMetaDataEntry.setInstanceId(coreInstanceMetaDataEntry.getInstanceId());
            instanceMetaDataEntry.setVolumeCount(coreInstanceMetaDataEntry.getVolumeCount());
            instanceMetaDataEntry.setLongName(coreInstanceMetaDataEntry.getLongName());
            instanceMetaDataEntry.setDockerSubnet(String.format("172.18.%s.1", coreInstanceMetaDataEntry.getPrivateIp().split("\\.")[LAST]));
            instanceMetaDataEntry.setContainerCount(coreInstanceMetaDataEntry.getContainerCount());
            instanceMetaDataEntry.setAmbariServer(Boolean.FALSE);
            instanceMetaDataEntry.setRemovable(true);
            instanceMetaDataEntry.setStack(stack);
            instanceMetaData.add(instanceMetaDataEntry);
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
