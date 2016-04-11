package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow2.SelectableEvent;

@Component("StackStartFinishedAction")
public class StackStartFinishedAction extends AbstractStackStartAction<StartInstancesResult> {
    @Inject
    private StackStartStopService stackStartStopService;

    public StackStartFinishedAction() {
        super(StartInstancesResult.class);
    }

    @Override
    protected void doExecute(StackStartStopContext context, StartInstancesResult payload, Map<Object, Object> variables) throws Exception {
        stackStartStopService.finishStackStart(context);
        sendEvent(context);
        sendEvent(context.getFlowId(), FlowPhases.METADATA_COLLECT.name(),
                new StackStatusUpdateContext(context.getStack().getId(), platform(context.getStack().cloudPlatform()), true));
    }

    @Override
    protected Selectable createRequest(StackStartStopContext context) {
        return new SelectableEvent(StackStartEvent.START_FINALIZED_EVENT.stringRepresentation());
    }
}
