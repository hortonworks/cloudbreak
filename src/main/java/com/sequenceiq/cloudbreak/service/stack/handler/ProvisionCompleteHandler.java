package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupContext;

import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class ProvisionCompleteHandler implements Consumer<Event<ProvisionComplete>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionCompleteHandler.class);

    @Autowired
    private MetadataSetupContext metadataSetupContext;

    @Autowired
    private RetryingStackUpdater retryingStackUpdater;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    @Override
    public void accept(Event<ProvisionComplete> event) {
        ProvisionComplete stackCreateComplete = event.getData();
        CloudPlatform cloudPlatform = stackCreateComplete.getCloudPlatform();
        Long stackId = stackCreateComplete.getStackId();
        Stack stack = stackRepository.findById(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Accepted {} event on stack.", ReactorConfig.PROVISION_COMPLETE_EVENT);
        Set<Resource> resourcesSet = event.getData().getResources();
        retryingStackUpdater.updateStackResources(stackId, resourcesSet);
        boolean result = metadataSetupContext.setupMetadata(cloudPlatform, stackId);
        if (result) {
            cloudbreakEventService.fireCloudbreakEvent(stackId, BillingStatus.BILLING_STARTED.name(), "Provision of stack is successfully finished");
        }
    }

}
