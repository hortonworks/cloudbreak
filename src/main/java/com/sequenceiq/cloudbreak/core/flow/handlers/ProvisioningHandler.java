package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.service.FlowFacade;

import reactor.event.Event;

@Component
public class ProvisioningHandler extends AbstractFlowHandler<ProvisioningContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningHandler.class);

    @Autowired
    private FlowFacade flowFacade;

    @Override
    protected Object execute(Event<ProvisioningContext> event) throws CloudbreakException {
        LOGGER.debug("Executing provisioning logic. Event: {}", event);
        ProvisioningContext provisioningContext = event.getData();
        provisioningContext = flowFacade.provision(provisioningContext);
        return provisioningContext;
    }

    @Override
    protected void handleErrorFlow(Throwable throwable, Object data) {
        LOGGER.debug("Handling error during provisioning. Exception {}, Data: {}", throwable.getClass(), data);

    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        LOGGER.debug("Assembling payload for the next phase. Data: {}", serviceResult);
        return serviceResult;
    }
}
