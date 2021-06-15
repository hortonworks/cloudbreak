package com.sequenceiq.freeipa.flow.freeipa.salt.update;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;

public enum SaltUpdateEvent implements FlowEvent {

    SALT_UPDATE_EVENT,
    BOOTSTRAP_MACHINES_FINISHED_EVENT(EventSelectorUtil.selector(BootstrapMachinesSuccess.class)),
    BOOTSTRAP_MACHINES_FAILED_EVENT(EventSelectorUtil.selector(BootstrapMachinesFailed.class)),
    UPDATE_ORCHESTRATOR_CONFIG_FINISHED_EVENT(EventSelectorUtil.selector(OrchestratorConfigSuccess.class)),
    UPDATE_ORCHESTRATOR_CONFIG_FAILED_EVENT(EventSelectorUtil.selector(OrchestratorConfigFailed.class)),
    HIGHSTATE_FINISHED_EVENT(EventSelectorUtil.selector(InstallFreeIpaServicesSuccess.class)),
    HIGHSTATE_FAILED_EVENT(EventSelectorUtil.selector(InstallFreeIpaServicesFailed.class)),
    SALT_UPDATE_FAILED_EVENT,
    SALT_UPDATE_FINISHED_EVENT,
    SALT_UPDATE_FAILURE_HANDLED_EVENT;

    private final String event;

    SaltUpdateEvent(String event) {
        this.event = event;
    }

    SaltUpdateEvent() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
