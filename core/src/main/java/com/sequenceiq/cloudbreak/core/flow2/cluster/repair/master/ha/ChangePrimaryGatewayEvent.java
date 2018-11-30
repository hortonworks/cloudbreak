package com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.WaitForAmbariServerFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.WaitForAmbariServerSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ChangePrimaryGatewayFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ChangePrimaryGatewaySuccess;

public enum ChangePrimaryGatewayEvent implements FlowEvent {
    CHANGE_PRIMARY_GATEWAY_TRIGGER_EVENT("CHANGE_PRIMARY_GATEWAY_TRIGGER_EVENT"),
    CHANGE_PRIMARY_GATEWAY_FINISHED(EventSelectorUtil.selector(ChangePrimaryGatewaySuccess.class)),
    CHANGE_PRIMARY_GATEWAY_FAILED(EventSelectorUtil.selector(ChangePrimaryGatewayFailed.class)),
    AMBARI_SERVER_STARTED(EventSelectorUtil.selector(WaitForAmbariServerSuccess.class)),
    AMBARI_SERVER_START_FAILED(EventSelectorUtil.selector(WaitForAmbariServerFailed.class)),
    CHANGE_PRIMARY_GATEWAY_FLOW_FINISHED("CHANGE_PRIMARY_GATEWAY_FLOW_FINISHED"),
    CHANGE_PRIMARY_GATEWAY_FAILURE_HANDLED("CHANGE_PRIMARY_GATEWAY_FAILURE_HANDLED");

    private final String event;

    ChangePrimaryGatewayEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
