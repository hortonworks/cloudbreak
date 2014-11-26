package com.sequenceiq.cloudbreak.service.stack.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.event.StackOperationFailure;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class StackUpdateFailureHandler implements Consumer<Event<StackOperationFailure>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpdateFailureHandler.class);

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private StackRepository stackRepository;

    @Override
    public void accept(Event<StackOperationFailure> event) {
        StackOperationFailure stackOperationFailure = event.getData();
        Long stackId = stackOperationFailure.getStackId();
        Stack byId = stackRepository.findById(stackId);
        MDCBuilder.buildMdcContext(byId);
        LOGGER.info("Accepted {} event.", ReactorConfig.STACK_UPDATE_FAILED_EVENT);
        String detailedMessage = stackOperationFailure.getDetailedMessage();
        stackUpdater.updateMetadataReady(stackId, true);
        Stack stack = stackUpdater.updateStackStatus(stackId, Status.AVAILABLE, "Stack update failed. " + detailedMessage);
        stackUpdater.updateStackStatusReason(stack.getId(), detailedMessage);
    }

}
