package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.CbLoggerFactory;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
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

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private Reactor reactor;

    @Autowired
    private GccStackUtil gccStackUtil;

    public void allocateRoles(Long stackId, Set<CoreInstanceMetaData> coreInstanceMetaData) {
        try {
            Stack stack = stackRepository.findById(stackId);
            CbLoggerFactory.buildMdcContext(stack);
            if (!stack.isMetadataReady()) {
                if (coreInstanceMetaData.size() != stack.getNodeCount()) {
                    throw new WrongMetadataException(String.format(
                            "Size of the collected metadata set does not equal the node count of the stack. [stack: '%s']", stack.getId()));
                }
                Set<InstanceMetaData> instanceMetaData = prepareInstanceMetaData(stack, coreInstanceMetaData);
                stackUpdater.updateStackMetaData(stackId, instanceMetaData);
                stackUpdater.updateMetadataReady(stackId, true);
                LOGGER.info("Publishing {} event", ReactorConfig.AMBARI_ROLE_ALLOCATION_COMPLETE_EVENT);
                reactor.notify(ReactorConfig.AMBARI_ROLE_ALLOCATION_COMPLETE_EVENT, Event.wrap(new AmbariRoleAllocationComplete(stack,
                        getAmbariIp(instanceMetaData))));
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
        CbLoggerFactory.buildMdcContext(stack);
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

    private String getAmbariIp(Set<InstanceMetaData> instanceMetaDatas) {
        for (InstanceMetaData instanceMetaData : instanceMetaDatas) {
            if (instanceMetaData.getAmbariServer()) {
                return instanceMetaData.getPublicIp();
            }
        }
        return null;
    }

    private Set<InstanceMetaData> prepareInstanceMetaData(Stack stack, Set<CoreInstanceMetaData> coreInstanceMetaData) {
        return prepareInstanceMetaData(stack, coreInstanceMetaData, 0);
    }

    private Set<InstanceMetaData> prepareInstanceMetaData(Stack stack, Set<CoreInstanceMetaData> coreInstanceMetaData, int startIndex) {
        Set<InstanceMetaData> instanceMetaData = new HashSet<>();
        int instanceIndex = startIndex;
        String ambariIp = null;
        for (CoreInstanceMetaData coreInstanceMetaDataEntry : coreInstanceMetaData) {
            InstanceMetaData instanceMetaDataEntry = new InstanceMetaData();
            instanceMetaDataEntry.setPrivateIp(coreInstanceMetaDataEntry.getPrivateIp());
            instanceMetaDataEntry.setPublicIp(coreInstanceMetaDataEntry.getPublicDns());
            instanceMetaDataEntry.setInstanceId(coreInstanceMetaDataEntry.getInstanceId());
            instanceMetaDataEntry.setVolumeCount(coreInstanceMetaDataEntry.getVolumeCount());
            instanceMetaDataEntry.setLongName(coreInstanceMetaDataEntry.getLongName());
            instanceMetaDataEntry.setInstanceIndex(instanceIndex);
            instanceMetaDataEntry.setDockerSubnet(String.format("172.18.%s.1", coreInstanceMetaDataEntry.getPrivateIp().split("\\.")[LAST]));
            instanceMetaDataEntry.setContainerCount(coreInstanceMetaDataEntry.getContainerCount());
            if (instanceIndex == 0) {
                instanceMetaDataEntry.setAmbariServer(Boolean.TRUE);
                instanceMetaDataEntry.setRemovable(false);
                ambariIp = instanceMetaDataEntry.getPublicIp();
                if (ambariIp == null) {
                    throw new WrongMetadataException(String.format("Public IP of Ambari server cannot be null [stack: '%s', instanceId: '%s' ]",
                            stack.getId(), coreInstanceMetaDataEntry.getInstanceId()));
                }
            } else {
                instanceMetaDataEntry.setAmbariServer(Boolean.FALSE);
                instanceMetaDataEntry.setRemovable(true);
            }
            instanceIndex++;
            instanceMetaDataEntry.setStack(stack);
            instanceMetaData.add(instanceMetaDataEntry);
        }
        return instanceMetaData;
    }

    private void notifyStackCreateFailed(Long stackId, String cause) {
        CbLoggerFactory.buildMdcContext();
        LOGGER.info("Publishing {} event ", ReactorConfig.STACK_CREATE_FAILED_EVENT, stackId);
        StackOperationFailure stackCreationFailure = new StackOperationFailure(stackId, cause);
        reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(stackCreationFailure));
    }

}
