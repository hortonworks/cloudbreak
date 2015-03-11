package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.TerminationContext;
import com.sequenceiq.cloudbreak.core.flow.service.FlowFacade;

import reactor.event.Event;

@Service
public class StackTerminationHandler extends AbstractFlowHandler<TerminationContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationHandler.class);

    @Autowired
    private FlowFacade flowFacade;

    @Override
    protected Object execute(Event<TerminationContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        TerminationContext context = (TerminationContext) flowFacade.terminateStack(event.getData());
        LOGGER.info("Cluster terminated. Context: {}", context);
        return context;
    }

    @Override
    protected void handleErrorFlow(Throwable throwable, Object data) {
        Event event = (Event) data;
        TerminationContext context = (TerminationContext) event.getData();
        CloudbreakException exc = (CloudbreakException) throwable;
        LOGGER.info("handleErrorFlow() for phase: {}", event.getKey());
        event.setData(new TerminationContext(context.getStackId(), context.getCloudPlatform(), exc.getMessage()));
    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        LOGGER.info("assemblePayload() for phase: {}", serviceResult);
        return serviceResult;
    }
}
