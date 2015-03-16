package com.sequenceiq.cloudbreak.core.flow.handlers;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow.service.StackFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.event.Event;

@Service
public class StackStartHandler extends AbstractFlowHandler<StackStatusUpdateContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartHandler.class);

    @Autowired
    private StackFacade stackFacade;

    @Override
    protected Object execute(Event<StackStatusUpdateContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        StackStatusUpdateContext stackStatusUpdateContext = (StackStatusUpdateContext) stackFacade.start(event.getData());
        LOGGER.info("Stack started. Context: {}", stackStatusUpdateContext);
        return stackStatusUpdateContext;
    }

    @Override
    protected void handleErrorFlow(Throwable throwable, Object data) {
        Event event = (Event) data;
        StackStatusUpdateContext context = (StackStatusUpdateContext) event.getData();
        CloudbreakException exc = (CloudbreakException) throwable;
        LOGGER.info("handleErrorFlow() for phase: {}", event.getKey());
        event.setData(new StackStatusUpdateContext(context.getStackId(), context.isStart(), exc.getMessage()));
    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        LOGGER.info("assemblePayload() for phase: {}", serviceResult);
        return serviceResult;
    }
}
