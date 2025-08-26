package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.selection.ChangePrimaryGatewaySelectionSuccess;
import com.sequenceiq.freeipa.flow.stack.HealthCheckFailed;
import com.sequenceiq.freeipa.flow.stack.HealthCheckSuccess;

public enum ChangePrimaryGatewayFlowEvent implements FlowEvent {
    CHANGE_PRIMARY_GATEWAY_EVENT("CHANGE_PRIMARY_GATEWAY_EVENT"),
    CHANGE_PRIMARY_GATEWAY_STARTING_FINISHED_EVENT("CHANGE_PRIMARY_GATEWAY_STARTING_FINISHED_EVENT"),
    CHANGE_PRIMARY_GATEWAY_SELECTION_FINISHED_EVENT(EventSelectorUtil.selector(ChangePrimaryGatewaySelectionSuccess.class)),
    CHANGE_PRIMARY_GATEWAY_SELECTION_FAILED_EVENT(EventSelectorUtil.selector(ChangePrimaryGatewayFailureEvent.class)),
    CHANGE_PRIMARY_GATEWAY_METADATA_FINISHED_EVENT("CHANGE_PRIMARY_GATEWAY_METADATA_FINISHED_EVENT"),
    CHANGE_PRIMARY_GATEWAY_METADATA_FAILED_EVENT("CHANGE_PRIMARY_GATEWAY_METADATA_FAILED_EVENT"),
    CHANGE_PRIMARY_GATEWAY_CLUSTERPROXY_REGISTRATION_FINISHED_EVENT(EventSelectorUtil.selector(ClusterProxyUpdateRegistrationSuccess.class)),
    CHANGE_PRIMARY_GATEWAY_CLUSTERPROXY_REGISTRATION_FAILED_EVENT(EventSelectorUtil.selector(ClusterProxyUpdateRegistrationFailed.class)),
    CHANGE_PRIMARY_GATEWAY_HEALTH_CHECK_FINISHED_EVENT(EventSelectorUtil.selector(HealthCheckSuccess.class)),
    CHANGE_PRIMARY_GATEWAY_HEALTH_CHECK_FAILED_EVENT(EventSelectorUtil.selector(HealthCheckFailed.class)),
    CHANGE_PRIMARY_GATEWAY_SWITCH_FREEIPA_MASTER_TO_PRIMARY_GATEWAY_FINISHED_EVENT,
    CHANGE_PRIMARY_GATEWAY_FINISHED_EVENT("CHANGE_PRIMARY_GATEWAY_FINISHED_EVENT"),
    FAILURE_EVENT(EventSelectorUtil.selector(ChangePrimaryGatewayFailureEvent.class)),
    FAIL_HANDLED_EVENT("CHANGE_PRIMARY_GATEWAY_FAIL_HANDLED_EVENT");

    private final String event;

    ChangePrimaryGatewayFlowEvent(String event) {
        this.event = event;
    }

    ChangePrimaryGatewayFlowEvent() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
