package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;

import reactor.bus.Event;
@Service
public class ClusterStatusUpdateFailureHandler extends AbstractFlowHandler<StackStatusUpdateContext> implements FlowHandler {

    @Override
    protected Object execute(Event<StackStatusUpdateContext> event) throws CloudbreakException {
        StackStatusUpdateContext context = event.getData();
        if (context.isStart()) {
            getFlowFacade().handleClusterStartFailure(context);
        } else {
            getFlowFacade().handleClusterStopFailure(context);
        }
        return context;
    }
}
