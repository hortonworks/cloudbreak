package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;

import reactor.bus.Event;
@Service
public class ClusterStartHandler extends AbstractFlowHandler<StackStatusUpdateContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartHandler.class);

    @Override
    protected Object execute(Event<StackStatusUpdateContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        StackStatusUpdateContext context = event.getData();
        return getFlowFacade().startCluster(context);
    }


    @Override
    protected Object handleErrorFlow(Throwable throwable, StackStatusUpdateContext data) throws Exception {
        return super.handleErrorFlow(throwable, data);
    }
}
