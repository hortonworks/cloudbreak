package com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate;

import com.sequenceiq.flow.core.FlowEvent;

public enum FreeIpaProviderTemplateUpdateFlowEvent implements FlowEvent {
    FREEIPA_PROVIDER_TEMPLATE_UPDATE_TRIGGER_EVENT("FREEIPA_PROVIDER_TEMPLATE_UPDATE_TRIGGER_EVENT"),
    FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT("FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT"),
    FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINALIZED_EVENT("FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINALIZED_EVENT"),
    FREEIPA_PROVIDER_TEMPLATE_UPDATE_FAILURE_EVENT("FREEIPA_PROVIDER_TEMPLATE_UPDATE_FAILURE_EVENT"),
    FREEIPA_PROVIDER_TEMPLATE_UPDATE_FAIL_HANDLED_EVENT("FREEIPA_PROVIDER_TEMPLATE_UPDATE_FAIL_HANDLED_EVENT");

    private final String event;

    FreeIpaProviderTemplateUpdateFlowEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }

    @Override
    public  String selector() {
        return event;
    }
}
