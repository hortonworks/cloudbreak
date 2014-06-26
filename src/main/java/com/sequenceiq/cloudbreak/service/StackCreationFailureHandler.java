package com.sequenceiq.cloudbreak.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.event.Event;
import reactor.function.Consumer;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.event.StackCreationFailure;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

@Service
public class StackCreationFailureHandler implements Consumer<Event<StackCreationFailure>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreationFailureHandler.class);

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private WebsocketService websocketService;

    @Override
    public void accept(Event<StackCreationFailure> event) {
        StackCreationFailure stackCreationFailure = event.getData();
        Stack stack = stackCreationFailure.getStack();
        LOGGER.info("Accepted {} event.", ReactorConfig.STACK_CREATE_FAILED_EVENT, stack.getId());
        String detailedMessage = stackCreationFailure.getDetailedMessage();
        stack = stackUpdater.updateStackStatus(stack.getId(), Status.CREATE_FAILED);
        websocketService.sendToTopicUser(stack.getUser().getEmail(), WebsocketEndPoint.STACK,
                new StatusMessage(stack.getId(), stack.getName(), Status.CREATE_FAILED.name(), detailedMessage));
    }

}
