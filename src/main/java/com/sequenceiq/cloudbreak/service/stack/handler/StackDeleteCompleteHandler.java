package com.sequenceiq.cloudbreak.service.stack.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteComplete;

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
    private CloudbreakEventService cloudbreakEventService;

    @Override
    public void accept(Event<StackDeleteComplete> stackDeleteComplete) {
        String msg = "Cluster and it's infrastructure were successfully deleted.";
        StackDeleteComplete data = stackDeleteComplete.getData();
        retryingStackUpdater.updateStackStatus(data.getStackId(), Status.DELETE_COMPLETED, msg);
        String statusReason = "Billing stopped because the deletion of cluster and its infrastructure.";
        cloudbreakEventService.fireCloudbreakEvent(data.getStackId(), BillingStatus.BILLING_STOPPED.name(), statusReason);
        Stack oneWithLists = stackRepository.findOneWithLists(data.getStackId());
        MDCBuilder.buildMdcContext(oneWithLists);
        LOGGER.info("Accepted {} event.", ReactorConfig.DELETE_COMPLETE_EVENT);
        stackRepository.delete(oneWithLists);
        retryingStackUpdater.updateStackStatusReason(oneWithLists.getId(), String.format(msg));
    }
}
