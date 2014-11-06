package com.sequenceiq.cloudbreak.service.stack.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteComplete;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class StackDeleteCompleteHandler implements Consumer<Event<StackDeleteComplete>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDeleteCompleteHandler.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater retryingStackUpdater;

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    @Override
    public void accept(Event<StackDeleteComplete> stackDeleteComplete) {
        StackDeleteComplete data = stackDeleteComplete.getData();
        retryingStackUpdater.updateStackStatus(data.getStackId(), Status.DELETE_COMPLETED);
        cloudbreakEventService.fireCloudbreakEvent(data.getStackId(), BillingStatus.BILLING_STOPPED.name(), "Stack delete completed");
        Stack oneWithLists = stackRepository.findOneWithLists(data.getStackId());
        MDCBuilder.buildMdcContext(oneWithLists);
        LOGGER.info("Accepted {} event.", ReactorConfig.DELETE_COMPLETE_EVENT);
        stackRepository.delete(oneWithLists);
        websocketService.sendToTopicUser(oneWithLists.getOwner(), WebsocketEndPoint.TERMINATE,
                new StatusMessage(oneWithLists.getId(), oneWithLists.getName(), Status.DELETE_COMPLETED.name(), String.format("Stack delete completed")));
        retryingStackUpdater.updateStackStatusReason(oneWithLists.getId(), String.format("Stack delete completed"));
    }
}
