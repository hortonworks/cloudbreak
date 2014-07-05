package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.StackCreationFailure;
import com.sequenceiq.cloudbreak.service.stack.WrongMetadataException;
import com.sequenceiq.cloudbreak.service.stack.event.CoreInstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSuccess;

@Component
public class MetadataSetupCompleteHandler implements Consumer<Event<MetadataSetupComplete>> {

    private static final String DOCKER_SUBNET_PREFIX = "172.17.1";

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataSetupCompleteHandler.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private Reactor reactor;

    @Override
    public void accept(Event<MetadataSetupComplete> event) {
        MetadataSetupComplete metadataSetupComplete = event.getData();
        Long stackId = metadataSetupComplete.getStackId();
        Set<CoreInstanceMetaData> coreInstanceMetaData = metadataSetupComplete.getCoreInstanceMetaData();
        LOGGER.info("Accepted {} event.", ReactorConfig.METADATA_SETUP_COMPLETE_EVENT, stackId);
        try {
            Stack stack = stackRepository.findById(stackId);
            if (!stack.isMetadataReady()) {
                if (coreInstanceMetaData.size() != stack.getNodeCount()) {
                    throw new WrongMetadataException(String.format(
                            "Size of the collected metadata set does not equal the node count of the stack. [stack: '%s']", stack.getId()));
                }
                Set<InstanceMetaData> instanceMetaData = new HashSet<>();
                int instanceIndex = 0;
                String ambariIp = null;
                for (CoreInstanceMetaData coreInstanceMetaDataEntry : coreInstanceMetaData) {
                    InstanceMetaData instanceMetaDataEntry = new InstanceMetaData();
                    instanceMetaDataEntry.setPrivateIp(coreInstanceMetaDataEntry.getPrivateIp());
                    instanceMetaDataEntry.setPublicIp(coreInstanceMetaDataEntry.getPublicIp());
                    instanceMetaDataEntry.setInstanceId(coreInstanceMetaDataEntry.getInstanceId());
                    instanceMetaDataEntry.setInstanceIndex(instanceIndex);
                    instanceMetaDataEntry.setDockerSubnet(DOCKER_SUBNET_PREFIX + instanceIndex);
                    if (instanceIndex == 0) {
                        instanceMetaDataEntry.setAmbariServer(Boolean.TRUE);
                        ambariIp = instanceMetaDataEntry.getPublicIp();
                        if (ambariIp == null) {
                            throw new WrongMetadataException(String.format("Public IP of Ambari server cannot be null [stack: '%s', instanceId: '%s' ]",
                                    stackId, coreInstanceMetaDataEntry.getInstanceId()));
                        }
                    } else {
                        instanceMetaDataEntry.setAmbariServer(Boolean.FALSE);
                    }
                    instanceIndex++;
                    instanceMetaDataEntry.setStack(stack);
                    instanceMetaData.add(instanceMetaDataEntry);
                }
                stackUpdater.updateStackMetaData(stackId, instanceMetaData);
                stackUpdater.updateMetadataReady(stackId);
                LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_SUCCESS_EVENT, stackId);
                reactor.notify(ReactorConfig.PROVISION_SUCCESS_EVENT, Event.wrap(new ProvisionSuccess(stackId, ambariIp)));
            } else {
                LOGGER.info("Metadata of stack '{}' is already created, ignoring '{}' event.", stackId, ReactorConfig.METADATA_SETUP_COMPLETE_EVENT);
            }
        } catch (WrongMetadataException e) {
            LOGGER.error(e.getMessage(), e);
            notifyStackCreateFailed(stackId, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unhandled exception occured while creating stack.", e);
            notifyStackCreateFailed(stackId, "Unhandled exception occured while creating stack.");
        }
    }

    private void notifyStackCreateFailed(Long stackId, String cause) {
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_CREATE_FAILED_EVENT, stackId);
        StackCreationFailure stackCreationFailure = new StackCreationFailure(stackId, cause);
        reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(stackCreationFailure));
    }
}
