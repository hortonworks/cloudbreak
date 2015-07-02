package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.DefaultFlowContext;

import reactor.bus.Event;

@Service
public class UpscaleStackSyncHandler extends AbstractFlowHandler<DefaultFlowContext> implements FlowHandler {
    @Override
    protected Object execute(Event<DefaultFlowContext> event) throws CloudbreakException {
        DefaultFlowContext context = event.getData();
        getFlowFacade().handleStackSync(context);
        return context;
    }
}