package com.sequenceiq.cloudbreak.service.stack.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.service.stack.StackCreationFailure;
import com.sequenceiq.cloudbreak.service.stack.StackCreationSuccess;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSuccess;

@Component
public class ProvisionSuccessHandler implements Consumer<Event<ProvisionSuccess>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataSetupCompleteHandler.class);

    @Autowired
    private Reactor reactor;

    @Override
    public void accept(Event<ProvisionSuccess> event) {
        ProvisionSuccess provisionSuccess = event.getData();
        Long stackId = provisionSuccess.getStackId();
        String ambariIp = provisionSuccess.getAmbariIp();
        LOGGER.info("Accepted {} event.", ReactorConfig.PROVISION_SUCCESS_EVENT, stackId);
        try {
            // TODO: poll ambari server until it's reachable
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_CREATE_SUCCESS_EVENT, stackId);
            reactor.notify(ReactorConfig.STACK_CREATE_SUCCESS_EVENT, Event.wrap(new StackCreationSuccess(stackId, ambariIp)));
        } catch (Exception e) {
            LOGGER.error("Unhandled exception occured while trying to reach initializing Ambari server.", e);
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_CREATE_FAILED_EVENT, stackId);
            StackCreationFailure stackCreationFailure = new StackCreationFailure(stackId,
                    "Unhandled exception occured while trying to reach initializing Ambari server.");
            reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(stackCreationFailure));
        }
    }
}
