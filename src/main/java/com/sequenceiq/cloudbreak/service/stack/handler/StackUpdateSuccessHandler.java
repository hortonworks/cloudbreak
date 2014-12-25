package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
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
        if (updateSuccess.isRemoveInstances()) {
            stackUpdater.updateNodeCount(stackId, stack.getTemplateAsGroup(updateSuccess.getHostGroup()).getNodeCount()  - instanceIds.size(),
                    updateSuccess.getHostGroup());
            Set<InstanceMetaData> metadataToRemove = new HashSet<>();
            for (InstanceMetaData metadataEntry : stack.getInstanceMetaData()) {
                for (String instanceId : instanceIds) {
                    if (metadataEntry.getInstanceId().equals(instanceId)) {
                        metadataToRemove.add(metadataEntry);
                    }
                }
            }
            stack.getInstanceMetaData().removeAll(metadataToRemove);
            stackUpdater.updateStackMetaData(stackId, stack.getInstanceMetaData());
            LOGGER.info("Successfully removed metadata of instances '{}' in stack.", instanceIds);
            eventService.fireCloudbreakEvent(stack.getId(), BillingStatus.BILLING_CHANGED.name(),
                    "Billing changed due to downscaling of cluster infrastructure.");
        } else {
            stackUpdater.updateNodeCount(stackId, stack.getTemplateAsGroup(updateSuccess.getHostGroup()).getNodeCount() + instanceIds.size(),
                    updateSuccess.getHostGroup());
            eventService.fireCloudbreakEvent(stackId, BillingStatus.BILLING_CHANGED.name(), "Billing changed due to upscaling of cluster infrastructure.");
        }
        stackUpdater.updateMetadataReady(stackId, true);
        String statusCause = String.format("%sscaling of cluster infrastructure was successfully.", updateSuccess.isRemoveInstances() ? "Down" : "Up");
        stackUpdater.updateStackStatus(stackId, Status.AVAILABLE, statusCause);

    }

}
