package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;

import reactor.event.Event;

@Service
public class ClusterStatusUpdateFailureHandler extends AbstractFlowHandler<StackStatusUpdateContext> {

    @Override
    protected Object execute(Event<StackStatusUpdateContext> event) throws CloudbreakException {
        StackStatusUpdateContext context = event.getData();
        if (context.isStart()) {
            getFlowFacade().clusterStartError(context);
        } else {
            getFlowFacade().clusterStopError(context);
        }
        return context;
    }

    @Override
    protected void handleErrorFlow(Throwable throwable, Object data) {
    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        return null;
    }
}
