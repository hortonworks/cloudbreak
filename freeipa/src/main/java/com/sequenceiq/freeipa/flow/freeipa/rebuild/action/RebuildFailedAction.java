package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.converter.DownscaleStackCollectResourcesResultToRebuildFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.converter.DownscaleStackResultToRebuildFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.converter.UpscaleStackResultToRebuildFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

/**
 * TODO
 */
@Component("RebuildFailedAction")
public class RebuildFailedAction extends AbstractRebuildAction<RebuildFailureEvent> {
    protected RebuildFailedAction() {
        super(RebuildFailureEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, RebuildFailureEvent payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_FAILED,
                "Failed to rebuild FreeIPA: " + getErrorReason(payload.getException()));
        sendEvent(context, new StackEvent(FreeIpaRebuildFlowEvent.REBUILD_FAILURE_HANDLED_EVENT.event(), payload.getResourceId()));
    }

    @Override
    protected void initPayloadConverterMap(List<PayloadConverter<RebuildFailureEvent>> payloadConverters) {
        payloadConverters.add(new UpscaleStackResultToRebuildFailureEvent());
        payloadConverters.add(new DownscaleStackCollectResourcesResultToRebuildFailureEvent());
        payloadConverters.add(new DownscaleStackResultToRebuildFailureEvent());
    }
}
