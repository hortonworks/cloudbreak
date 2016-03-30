package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.core.flow2.SelectableEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopService;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;

@Component("StackStopFinishedAction")
public class StackStopFinishedAction extends AbstractStackStopAction<StopInstancesResult> {
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private EmailSenderService emailSenderService;
    @Inject
    private FlowMessageService flowMessageService;
    @Inject
    private StackStartStopService stackStartStopService;

    public StackStopFinishedAction() {
        super(StopInstancesResult.class);
    }

    @Override
    protected Long getStackId(StopInstancesResult payload) {
        return payload.getCloudContext().getId();
    }

    @Override
    protected void doExecute(StackStartStopContext context, StopInstancesResult payload, Map<Object, Object> variables) throws Exception {
        stackStartStopService.finishStackStop(context);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackStartStopContext context) {
        return new SelectableEvent(StackStopEvent.STOP_FINALIZED_EVENT.stringRepresentation());
    }
}
