package com.sequenceiq.cloudbreak.core.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.service.stack.flow.ProvisioningService;

import reactor.event.Event;

public class ProvisioningHandler extends AbstractFlowHandler<Event<Object>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningHandler.class);

    private ProvisioningService provisioningService;

    @Override protected Object execute(Event<Event<Object>> event) throws Exception {
        return null;
    }

    @Override protected void handleErrorFlow(Throwable throwable, Object data) {

    }

    @Override protected Object assemblePayload(Object serviceResult) {
        return null;
    }
}
