package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.ProvisioningContext;

import reactor.event.Event;

@Component
public class AmbariStartHandler extends AbstractFlowHandler<ProvisioningContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariStartHandler.class);

    @Override
    protected Object execute(Event<ProvisioningContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        return event;
    }

    @Override
    protected void handleErrorFlow(Throwable throwable, Object data) {
        LOGGER.info("handleErrorFlow() for phase: {}", ((Event) data).getKey());

    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        LOGGER.info("assemblePayload() for phase: {}", ((Event) serviceResult).getKey());
        return serviceResult;
    }
}
