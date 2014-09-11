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
import com.sequenceiq.cloudbreak.service.stack.event.StackOperationFailure;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class StackUpdateFailureHandler implements Consumer<Event<StackOperationFailure>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpdateFailureHandler.class);

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private WebsocketService websocketService;

    @Override
    public void accept(Event<StackOperationFailure> event) {
        StackOperationFailure stackOperationFailure = event.getData();
        Long stackId = stackOperationFailure.getStackId();
        LOGGER.info("Accepted {} event.", ReactorConfig.STACK_UPDATE_FAILED_EVENT, stackId);
        String detailedMessage = stackOperationFailure.getDetailedMessage();
        stackUpdater.updateMetadataReady(stackId, true);
        Stack stack = stackUpdater.updateStackStatus(stackId, Status.AVAILABLE, "Stack update failed. " + detailedMessage);
        websocketService.sendToTopicUser(stack.getOwner(), WebsocketEndPoint.STACK,
                new StatusMessage(stackId, stack.getName(), "UPDATE_FAILED", detailedMessage));
    }

}
