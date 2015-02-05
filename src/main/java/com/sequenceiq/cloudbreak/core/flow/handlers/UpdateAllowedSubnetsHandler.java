package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.UpdateAllowedSubnetsContext;

import reactor.event.Event;

@Component
public class UpdateAllowedSubnetsHandler extends AbstractFlowHandler<UpdateAllowedSubnetsContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAllowedSubnetsHandler.class);

    @Override
    protected Object execute(Event<UpdateAllowedSubnetsContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        FlowContext context = getFlowFacade().updateAllowedSubnets(event.getData());
        LOGGER.info("Allowed subnets are updated. Context: {}", context);
        return context;
    }

    @Override
    protected Object handleErrorFlow(Throwable throwable, UpdateAllowedSubnetsContext data) throws Exception {
        LOGGER.info("execute() for phase: {}", getClass());
        data.setErrorReason(throwable.getMessage());
        FlowContext context = getFlowFacade().handleUpdateAllowedSubnetsFailure(data);
        LOGGER.info("Stack termination failure is handled. Context: {}", context);
        return context;
    }
}
