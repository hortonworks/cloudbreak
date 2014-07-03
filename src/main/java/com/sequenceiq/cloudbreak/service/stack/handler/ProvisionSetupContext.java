package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.StackCreationFailure;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;

@Component
public class ProvisionSetupContext implements Consumer<Event<ProvisionRequest>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionSetupContext.class);

    @Autowired
    private StackRepository stackRepository;

    @Resource
    private Map<CloudPlatform, ProvisionSetup> provisionSetups;

    @Autowired
    private Reactor reactor;

    @Override
    public void accept(Event<ProvisionRequest> event) {
        ProvisionRequest provisionRequest = event.getData();
        CloudPlatform cloudPlatform = provisionRequest.getCloudPlatform();
        Long stackId = provisionRequest.getStackId();
        LOGGER.info("Accepted {} event.", ReactorConfig.PROVISION_REQUEST_EVENT, stackId);
        try {
            ProvisionSetup provisionSetup = provisionSetups.get(cloudPlatform);
            provisionSetup.setupProvisioning(stackRepository.findById(stackId));
        } catch (Exception e) {
            LOGGER.error("Unhandled exception occured while setting up provisioning.", e);
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_CREATE_FAILED_EVENT, stackId);
            reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(new StackCreationFailure(stackId,
                    "Internal server error occured while setting up provisioning.")));
        }
    }
}
