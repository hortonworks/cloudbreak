package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.event.StackUpdateSuccess;

import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class StackUpdateSuccessHandler implements Consumer<Event<StackUpdateSuccess>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpdateSuccessHandler.class);

    private static final int POLLING_INTERVAL = 5000;
    private static final int MAX_POLLING_ATTEMPTS = 100;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private CloudbreakEventService eventService;

    @Override
    public void accept(Event<StackUpdateSuccess> t) {
        StackUpdateSuccess updateSuccess = t.getData();
        Long stackId = updateSuccess.getStackId();
        Stack stack = stackRepository.findOneWithLists(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Accepted {} event.", ReactorConfig.STACK_UPDATE_SUCCESS_EVENT);
        Set<String> instanceIds = updateSuccess.getInstanceIds();
        InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(updateSuccess.getInstanceGroup());
        if (updateSuccess.isRemoveInstances()) {
            eventService.fireCloudbreakEvent(stack.getId(), BillingStatus.BILLING_CHANGED.name(),
                    "Billing changed due to downscaling of cluster infrastructure.");
        } else {
            int nodeCount = instanceGroup.getNodeCount() + instanceIds.size();
            stackUpdater.updateNodeCount(stackId, nodeCount, updateSuccess.getInstanceGroup());
            eventService.fireCloudbreakEvent(stackId, BillingStatus.BILLING_CHANGED.name(), "Billing changed due to upscaling of cluster infrastructure.");
        }
        stackUpdater.updateMetadataReady(stackId, true);
        String statusCause = String.format("%sscaling of cluster infrastructure was successful.", updateSuccess.isRemoveInstances() ? "Down" : "Up");
        stackUpdater.updateStackStatus(stackId, Status.AVAILABLE, statusCause);

    }


}
