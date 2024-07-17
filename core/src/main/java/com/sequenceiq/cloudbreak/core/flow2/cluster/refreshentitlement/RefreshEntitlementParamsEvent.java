package com.sequenceiq.cloudbreak.core.flow2.cluster.refreshentitlement;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RefreshEntitlementParamsTriggerEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum RefreshEntitlementParamsEvent implements FlowEvent {

    REFRESH_ENTITLEMENT_PARAMS_TRIGGER_EVENT(EventSelectorUtil.selector(RefreshEntitlementParamsTriggerEvent.class)),

    CONFIGURE_MANAGEMENT_SERVICES_EVENT,
    CONFIGURE_MANAGEMENT_SERVICES_SUCCESS_EVENT(EventSelectorUtil.selector(ConfigureClusterManagerManagementServicesSuccess.class)),
    CONFIGURE_MANAGEMENT_SERVICES_FAILED_EVENT(EventSelectorUtil.selector(ConfigureClusterManagerManagementServicesFailed.class)),

    REFRESH_ENTITLEMENT_FINALIZED_EVENT,
    REFRESH_ENTITLEMENT_FAILURE_EVENT,
    REFRESH_ENTITLEMENT_FAIL_HANDLED_EVENT;

    private final String event;

    RefreshEntitlementParamsEvent(String event) {
        this.event = event;
    }

    RefreshEntitlementParamsEvent() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
