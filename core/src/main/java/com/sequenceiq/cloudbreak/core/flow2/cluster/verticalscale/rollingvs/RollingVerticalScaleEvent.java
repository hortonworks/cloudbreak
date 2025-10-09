package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleInstancesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStartInstancesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStopInstancesResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum RollingVerticalScaleEvent implements FlowEvent {

    ROLLING_VERTICALSCALE_TRIGGER_EVENT("ROLLING_VERTICALSCALE_TRIGGER_EVENT"),
    ROLLING_VERTICALSCALE_SCALE_INSTANCES_EVENT(EventSelectorUtil.selector(RollingVerticalScaleStopInstancesResult.class)),
    ROLLING_VERTICALSCALE_START_INSTANCES_EVENT(EventSelectorUtil.selector(RollingVerticalScaleInstancesResult.class)),
    ROLLING_VERTICALSCALE_FINISHED_EVENT(EventSelectorUtil.selector(RollingVerticalScaleStartInstancesResult.class)),
    ROLLING_VERTICALSCALE_FINALIZED_EVENT("ROLLING_VERTICALSCALE_FINALIZED_EVENT"),
    ROLLING_VERTICALSCALE_FAILURE_EVENT("ROLLING_VERTICALSCALE_FAILURE_EVENT"),
    ROLLING_VERTICALSCALE_FAIL_HANDLED_EVENT("ROLLING_VERTICALSCALE_FAIL_HANDLED_EVENT");

    private final String event;

    RollingVerticalScaleEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
