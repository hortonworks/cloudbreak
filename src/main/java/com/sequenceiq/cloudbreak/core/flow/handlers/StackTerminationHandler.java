package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.TerminationContext;
import com.sequenceiq.cloudbreak.core.flow.service.FlowFacade;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationFailedException;

import reactor.event.Event;

public class StackTerminationHandler extends AbstractFlowHandler<TerminationContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationHandler.class);

    @Autowired
    private FlowFacade flowFacade;

    @Override
    protected Object execute(Event<TerminationContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        ProvisioningContext provisioningContext = (ProvisioningContext) flowFacade.terminateStack(event.getData());
        LOGGER.info("Cluster terminated. Context: {}", provisioningContext);
        return provisioningContext;
    }

    @Override
    protected void handleErrorFlow(Throwable throwable, Object data) {
        Event event = (Event) data;
        TerminationContext context = (TerminationContext) event.getData();
        TerminationFailedException exc = (TerminationFailedException) throwable;
        LOGGER.info("handleErrorFlow() for phase: {}", event.getKey());
        event.setData(new TerminationContext(context.getStackId(), context.getCloudPlatform(), exc.getMessage()));

    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        LOGGER.info("assemblePayload() for phase: {}", serviceResult);
        return serviceResult;
    }
}
