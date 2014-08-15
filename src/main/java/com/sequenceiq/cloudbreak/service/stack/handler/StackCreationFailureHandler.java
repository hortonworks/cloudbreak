package com.sequenceiq.cloudbreak.service.stack.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClusterInstallerMailSenderService;
import com.sequenceiq.cloudbreak.service.stack.event.StackCreationFailure;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class StackCreationFailureHandler implements Consumer<Event<StackCreationFailure>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreationFailureHandler.class);

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private AmbariClusterInstallerMailSenderService ambariClusterInstallerMailSenderService;

    @Override
    public void accept(Event<StackCreationFailure> event) {
        StackCreationFailure stackCreationFailure = event.getData();
        Long stackId = stackCreationFailure.getStackId();
        LOGGER.info("Accepted {} event.", ReactorConfig.STACK_CREATE_FAILED_EVENT, stackId);
        String detailedMessage = stackCreationFailure.getDetailedMessage();
        Stack stack = stackUpdater.updateStackStatus(stackId, Status.CREATE_FAILED, detailedMessage);
        if (stack.getCluster().getEmailNeeded()) {
            ambariClusterInstallerMailSenderService.sendFailEmail(stack.getUser());
        }
        websocketService.sendToTopicUser(stack.getUser().getEmail(), WebsocketEndPoint.STACK,
                new StatusMessage(stackId, stack.getName(), Status.CREATE_FAILED.name(), detailedMessage));
    }

}
