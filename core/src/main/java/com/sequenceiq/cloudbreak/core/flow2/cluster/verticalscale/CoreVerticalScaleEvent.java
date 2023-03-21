package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScalePreparationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScalePreparationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScaleResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum CoreVerticalScaleEvent implements FlowEvent {
    STACK_VERTICALSCALE_EVENT("STACK_VERTICAL_SCALE_TRIGGER_EVENT"),
    STACK_VERTICALSCALE_PREPARATION_EVENT(EventSelectorUtil.selector(CoreVerticalScalePreparationRequest.class)),
    STACK_VERTICALSCALE_PREPARATION_FINISHED_EVENT(EventSelectorUtil.selector(CoreVerticalScalePreparationResult.class)),
    STACK_VERTICALSCALE_PREPARATION_FAILURE_EVENT(EventSelectorUtil.failureSelector(CoreVerticalScalePreparationResult.class)),
    STACK_VERTICALSCALE_FINISHED_EVENT(EventSelectorUtil.selector(CoreVerticalScaleResult.class)),
    STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(CoreVerticalScaleResult.class)),

    FINALIZED_EVENT("STACKVERTICALSCALEFINALIZEDEVENT"),
    FAILURE_EVENT("STACKVERTICALSCALEFAILUREEVENT"),
    FAIL_HANDLED_EVENT("STACKVERTICALSCALEFAILHANDLEDEVENT");

    private final String event;

    CoreVerticalScaleEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
