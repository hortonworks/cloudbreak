package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;

import reactor.bus.Event;

@Component
public class CheckImageStatusHandler extends AbstractFlowHandler<ProvisioningContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckImageStatusHandler.class);

    @Override
    protected Object execute(Event<ProvisioningContext> event) throws CloudbreakException {
        LOGGER.info("Executing check image logic. Event: {}", event);
        FlowContext provisioningContext = event.getData();
        provisioningContext = getFlowFacade().checkImage(provisioningContext);
        return provisioningContext;
    }
}
