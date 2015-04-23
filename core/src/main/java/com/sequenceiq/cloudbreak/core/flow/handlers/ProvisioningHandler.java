package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;

import reactor.event.Event;

@Component
public class ProvisioningHandler extends AbstractFlowHandler<ProvisioningContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningHandler.class);

    @Override
    protected Object execute(Event<ProvisioningContext> event) throws CloudbreakException {
        LOGGER.debug("Executing provisioning logic. Event: {}", event);
        FlowContext provisioningContext = event.getData();
        provisioningContext = getFlowFacade().provision(provisioningContext);
        return provisioningContext;
    }
}
