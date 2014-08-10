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
import com.sequenceiq.cloudbreak.service.stack.event.StackCreationSuccess;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class StackCreationSuccessHandler implements Consumer<Event<StackCreationSuccess>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreationSuccessHandler.class);

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private Reactor reactor;

    @Override
    public void accept(Event<StackCreationSuccess> event) {
        StackCreationSuccess stackCreationSuccess = event.getData();
        Long stackId = stackCreationSuccess.getStackId();
        LOGGER.info("Accepted {} event.", ReactorConfig.STACK_CREATE_SUCCESS_EVENT, stackId);
        String ambariIp = stackCreationSuccess.getAmbariIp();
        Stack stack = stackUpdater.updateAmbariIp(stackId, ambariIp);
        stack = stackUpdater.updateStackStatus(stackId, Status.CREATE_COMPLETED);
        websocketService.sendToTopicUser(stack.getUser().getEmail(), WebsocketEndPoint.STACK,
                new StatusMessage(stackId, stack.getName(), Status.CREATE_COMPLETED.name()));
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.AMBARI_STARTED_EVENT, stackId);
        reactor.notify(ReactorConfig.AMBARI_STARTED_EVENT, Event.wrap(stack));
    }

}
