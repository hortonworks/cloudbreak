package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.AddInstancesFailedException;
import com.sequenceiq.cloudbreak.service.stack.connector.Provisioner;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.event.StackOperationFailure;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateInstancesRequest;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class UpdateInstancesRequestHandler implements Consumer<Event<UpdateInstancesRequest>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateInstancesRequestHandler.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Resource
    private Map<CloudPlatform, Provisioner> provisioners;

    @Autowired
    private UserDataBuilder userDataBuilder;

    @Autowired
    private Reactor reactor;

    @Override
    public void accept(Event<UpdateInstancesRequest> event) {
        UpdateInstancesRequest request = event.getData();
        CloudPlatform cloudPlatform = request.getCloudPlatform();
        Long stackId = request.getStackId();
        Integer scalingAdjustment = request.getScalingAdjustment();
        try {
            Stack stack = stackRepository.findOneWithLists(stackId);
            LOGGER.info("Accepted {} event on stack: '{}'", ReactorConfig.UPDATE_INSTANCES_REQUEST_EVENT, stackId);
            stackUpdater.updateMetadataReady(stackId, false);
            if (scalingAdjustment > 0) {
                provisioners.get(cloudPlatform)
                        .addInstances(stack, userDataBuilder.build(cloudPlatform, stack.getHash(), new HashMap<String, String>()), scalingAdjustment);
            } else {
                Set<String> instanceIds = new HashSet<>();
                int i = 0;
                for (InstanceMetaData metadataEntry : stack.getInstanceMetaData()) {
                    if (metadataEntry.isRemovable()) {
                        instanceIds.add(metadataEntry.getInstanceId());
                        if (++i >= scalingAdjustment * -1) {
                            break;
                        }
                    }
                }
                provisioners.get(cloudPlatform).removeInstances(stack, instanceIds);
            }
        } catch (AddInstancesFailedException e) {
            LOGGER.error(e.getMessage(), e);
            notifyUpdateFailed(stackId, e.getMessage());
        } catch (Exception e) {
            String errMessage = "Unhandled exception occurred while updating stack.";
            LOGGER.error(errMessage, e);
            notifyUpdateFailed(stackId, errMessage);
        }
    }

    private void notifyUpdateFailed(Long stackId, String detailedMessage) {
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_UPDATE_FAILED_EVENT, stackId);
        reactor.notify(ReactorConfig.STACK_UPDATE_FAILED_EVENT, Event.wrap(new StackOperationFailure(stackId, detailedMessage)));
    }
}
