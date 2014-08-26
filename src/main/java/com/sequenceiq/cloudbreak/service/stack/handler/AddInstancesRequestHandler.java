package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.AddInstancesFailedException;
import com.sequenceiq.cloudbreak.service.stack.connector.Provisioner;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.event.AddInstancesRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackOperationFailure;

@Component
public class AddInstancesRequestHandler implements Consumer<Event<AddInstancesRequest>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddInstancesRequestHandler.class);

    @Autowired
    private StackRepository stackRepository;

    @Resource
    private Map<CloudPlatform, Provisioner> provisioners;

    @Autowired
    private UserDataBuilder userDataBuilder;

    @Autowired
    private Reactor reactor;

    @Override
    public void accept(Event<AddInstancesRequest> event) {
        AddInstancesRequest request = event.getData();
        CloudPlatform cloudPlatform = request.getCloudPlatform();
        Long stackId = request.getStackId();
        Integer scalingAdjustment = request.getScalingAdjustment();
        try {
            Stack one = stackRepository.findOneWithLists(stackId);
            LOGGER.info("Accepted {} event on stack: '{}'", ReactorConfig.ADD_INSTANCES_REQUEST_EVENT, stackId);
            provisioners.get(cloudPlatform)
                    .addNode(one, userDataBuilder.build(cloudPlatform, one.getHash(), new HashMap<String, String>()), scalingAdjustment);
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
