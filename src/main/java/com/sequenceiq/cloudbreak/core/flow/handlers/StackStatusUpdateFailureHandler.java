package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow.service.StackFacade;

import reactor.event.Event;

@Service
public class StackStatusUpdateFailureHandler extends AbstractFlowHandler<StackStatusUpdateContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusUpdateFailureHandler.class);

    @Autowired
    private StackFacade stackFacade;

    @Override
    protected Object execute(Event<StackStatusUpdateContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        StackStatusUpdateContext context = event.getData();
        if (context.isStart()) {
            stackFacade.stackStartError(context);
        } else {
            stackFacade.stackStopError(context);
        }
        return context;
    }

    @Override
    protected Object handleErrorFlow(Throwable throwable, Object data) {
        return data;
    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        return serviceResult;
    }
}
