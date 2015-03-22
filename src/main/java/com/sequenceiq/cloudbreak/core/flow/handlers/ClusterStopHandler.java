package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;

import reactor.event.Event;

@Service
public class ClusterStopHandler extends AbstractFlowHandler<StackStatusUpdateContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStopHandler.class);

    @Override
    protected Object execute(Event<StackStatusUpdateContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        StackStatusUpdateContext context = event.getData();
        getFlowFacade().stopCluster(context);
        return context;
    }

    @Override
    protected Object handleErrorFlow(Throwable throwable, Object data) {
        Event event = (Event) data;
        StackStatusUpdateContext context = (StackStatusUpdateContext) event.getData();
        CloudbreakException ex = (CloudbreakException) throwable;
        LOGGER.info("handleErrorFlow() for phase: {}", event.getKey());
        return new StackStatusUpdateContext(context.getStackId(), context.isStart(), ex.getMessage());

    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        LOGGER.info("assemblePayload() for phase: {}", serviceResult);
        return serviceResult;
    }
}
