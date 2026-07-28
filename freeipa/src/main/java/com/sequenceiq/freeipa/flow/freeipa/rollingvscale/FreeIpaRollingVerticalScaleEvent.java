package com.sequenceiq.freeipa.flow.freeipa.rollingvscale;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleDescribeResult;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleHealthCheckResult;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleResizeResult;
import com.sequenceiq.freeipa.flow.stack.stop.StackStopEvent;

public enum FreeIpaRollingVerticalScaleEvent implements FlowEvent {

    ROLLING_VERTICAL_SCALE_TRIGGER_EVENT,
    ROLLING_VERTICAL_SCALE_DESCRIBE_FINISHED_EVENT(EventSelectorUtil.selector(RollingVerticalScaleDescribeResult.class)),
    ROLLING_VERTICAL_SCALE_DESCRIBE_SKIP_EVENT,
    ROLLING_VERTICAL_SCALE_STOP_HEALTH_AGENT_FINISHED_EVENT(DownscaleFlowEvent.STOP_HEALTH_AGENT_FINISHED.event()),
    ROLLING_VERTICAL_SCALE_STOP_SERVICES_FINISHED_EVENT(StackStopEvent.STACK_STOP_INSTANCES_EVENT.event()),
    ROLLING_VERTICAL_SCALE_STOP_VM_FINISHED_EVENT(CloudPlatformResult.selector(StopInstancesResult.class)),
    ROLLING_VERTICAL_SCALE_STOP_VM_FAILURE_EVENT(CloudPlatformResult.failureSelector(StopInstancesResult.class)),
    ROLLING_VERTICAL_SCALE_RESIZE_FINISHED_EVENT(EventSelectorUtil.selector(RollingVerticalScaleResizeResult.class)),
    ROLLING_VERTICAL_SCALE_START_VM_FINISHED_EVENT(CloudPlatformResult.selector(StartInstancesResult.class)),
    ROLLING_VERTICAL_SCALE_START_VM_FAILURE_EVENT(CloudPlatformResult.failureSelector(StartInstancesResult.class)),
    ROLLING_VERTICAL_SCALE_HEALTH_CHECK_FINISHED_EVENT(EventSelectorUtil.selector(RollingVerticalScaleHealthCheckResult.class)),
    ROLLING_VERTICAL_SCALE_FINALIZED_EVENT,
    ROLLING_VERTICAL_SCALE_FAILURE_EVENT,
    ROLLING_VERTICAL_SCALE_FAIL_HANDLED_EVENT;

    private final String event;

    FreeIpaRollingVerticalScaleEvent(String event) {
        this.event = event;
    }

    FreeIpaRollingVerticalScaleEvent() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
