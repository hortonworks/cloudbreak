package com.sequenceiq.cloudbreak.service.stack.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisionContext;

import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class ProvisionSetupCompleteHandler implements Consumer<Event<ProvisionSetupComplete>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionSetupCompleteHandler.class);

    @Autowired
    private ProvisionContext provisionContext;

    @Autowired
    private StackRepository stackRepository;

    @Override
    public void accept(Event<ProvisionSetupComplete> event) {
        ProvisionSetupComplete provisionSetupComplete = event.getData();
        CloudPlatform cloudPlatform = provisionSetupComplete.getCloudPlatform();
        Long stackId = provisionSetupComplete.getStackId();
        Stack stack = stackRepository.findById(stackId);
        MDC.put(LoggerContextKey.OWNER_ID.toString(), stack.getOwner());
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), stack.getId().toString());
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.STACK_ID.toString());
        LOGGER.info("Accepted {} event.", ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT, stackId);
        provisionContext.buildStack(cloudPlatform, stackId, provisionSetupComplete.getSetupProperties(), provisionSetupComplete.getUserDataParams());
    }
}
