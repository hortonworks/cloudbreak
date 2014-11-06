package com.sequenceiq.cloudbreak.service.stack.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisionSetupContext;

import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class ProvisionRequestHandler implements Consumer<Event<ProvisionRequest>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionRequestHandler.class);

    @Autowired
    private ProvisionSetupContext provisionSetupContext;

    @Autowired
    private StackRepository stackRepository;

    @Override
    public void accept(Event<ProvisionRequest> event) {
        ProvisionRequest provisionRequest = event.getData();
        CloudPlatform cloudPlatform = provisionRequest.getCloudPlatform();
        Long stackId = provisionRequest.getStackId();
        Stack stack = stackRepository.findById(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Accepted {} event.", ReactorConfig.PROVISION_REQUEST_EVENT, stackId);
        provisionSetupContext.setupProvisioning(cloudPlatform, stackId);
    }
}
