package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.SelectableFlowStackEvent;

@Component("StackStartFailedAction")
public class StackStartFailedAction extends AbstractStackStartAction<StartInstancesResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartFailedAction.class);
    @Inject
    private StackStartStopService stackStartStopService;

    public StackStartFailedAction() {
        super(StartInstancesResult.class);
    }

    @Override
    protected Object getFailurePayload(StackStartStopContext flowContext, Exception ex) {
        return null;
    }

    @Override
    protected void doExecute(StackStartStopContext context, StartInstancesResult payload, Map<Object, Object> variables) throws Exception {
        stackStartStopService.handleStackStartError(context, payload);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackStartStopContext context) {
        return new SelectableFlowStackEvent(context.getStack().getId(), StackStartEvent.START_FAIL_HANDLED_EVENT.stringRepresentation());
    }
}
