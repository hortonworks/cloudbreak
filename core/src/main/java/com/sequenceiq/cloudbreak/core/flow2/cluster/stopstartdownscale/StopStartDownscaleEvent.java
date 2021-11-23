package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartdownscale;

import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StopStartDownscaleDecommissionViaCMResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleStopInstancesResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum StopStartDownscaleEvent implements FlowEvent {

    STOPSTART_DOWNSCALE_TRIGGER_EVENT("STOPSTART_DOWNSCALE_TRIGGER_EVENT"),
    STOPSTART_DOWNSCALE_CLUSTER_MANAGER_DECOMMISSIONED_EVENT(EventSelectorUtil.selector(StopStartDownscaleDecommissionViaCMResult.class)),
    STOPSTART_DOWNSCALE_INSTANCES_STOPPED_EVENT(EventSelectorUtil.selector(StopStartDownscaleStopInstancesResult.class)),
    STOPSTART_DOWNSCALE_FINALIZED_EVENT("STOPSTART_DOWNSCALE_FINALIZED_EVENT"),
    STOPSTART_DOWNSCALE_FAILURE_EVENT("STOPSTART_DOWNSCALE_FAILURE_EVENT"),
    STOPSTART_DOWNSCALE_FAIL_HANDLE_EVENT("STOPSTART_DOWNSCALE_FAIL_HANDLE_EVENT");



    private final String event;

    StopStartDownscaleEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
