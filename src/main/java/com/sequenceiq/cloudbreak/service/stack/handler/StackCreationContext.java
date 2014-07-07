package com.sequenceiq.cloudbreak.service.stack.handler;

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
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.Provisioner;
import com.sequenceiq.cloudbreak.service.stack.StackCreationFailure;
import com.sequenceiq.cloudbreak.service.stack.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

@Component
public class StackCreationContext implements Consumer<Event<ProvisionSetupComplete>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreationContext.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private WebsocketService websocketService;

    @Resource
    private Map<CloudPlatform, Provisioner> provisioners;

    @Autowired
    private Reactor reactor;

    @Autowired
    private UserDataBuilder userDataBuilder;

    @Override
    public void accept(Event<ProvisionSetupComplete> event) {
        ProvisionSetupComplete provisionSetupComplete = event.getData();
        CloudPlatform cloudPlatform = provisionSetupComplete.getCloudPlatform();
        Long stackId = provisionSetupComplete.getStackId();
        LOGGER.info("Accepted {} event.", ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT, stackId);
        try {
            Stack stack = stackRepository.findById(stackId);
            if (stack.getStatus().equals(Status.REQUESTED)) {
                stack = stackUpdater.updateStackStatus(stack.getId(), Status.CREATE_IN_PROGRESS);
                websocketService.sendToTopicUser(stack.getUser().getEmail(), WebsocketEndPoint.STACK, new StatusMessage(stack.getId(), stack.getName(), stack
                        .getStatus().name()));
                Provisioner provisioner = provisioners.get(cloudPlatform);
                provisioner.buildStack(
                        stack,
                        userDataBuilder.build(cloudPlatform, stack.getHash(), provisionSetupComplete.getUserDataParams()),
                        provisionSetupComplete.getSetupProperties()
                        );
            } else {
                LOGGER.info("CloudFormation stack creation was requested for a stack, that is not in REQUESTED status anymore. [stackId: '{}', status: '{}']",
                        stack.getId(), stack.getStatus());
            }
        } catch (Exception e) {
            LOGGER.error("Unhandled exception occured while creating stack.", e);
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_CREATE_FAILED_EVENT, stackId);
            StackCreationFailure stackCreationFailure = new StackCreationFailure(stackId,
                    "Internal server error occured while creating stack.");
            reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(stackCreationFailure));
        }
    }
}
