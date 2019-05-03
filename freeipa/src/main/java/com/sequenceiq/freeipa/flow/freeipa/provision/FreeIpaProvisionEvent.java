package com.sequenceiq.freeipa.flow.freeipa.provision;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.freeipa.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.hostmetadatasetup.HostMetadataSetupFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.hostmetadatasetup.HostMetadataSetupSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;

public enum FreeIpaProvisionEvent implements FlowEvent {
    FREEIPA_PROVISION_EVENT("FREEIPA_PROVISION_EVENT"),
    BOOTSTRAP_MACHINES_FINISHED_EVENT(EventSelectorUtil.selector(BootstrapMachinesSuccess.class)),
    BOOTSTRAP_MACHINES_FAILED_EVENT(EventSelectorUtil.selector(BootstrapMachinesFailed.class)),
    HOST_METADATASETUP_FINISHED_EVENT(EventSelectorUtil.selector(HostMetadataSetupSuccess.class)),
    HOST_METADATASETUP_FAILED_EVENT(EventSelectorUtil.selector(HostMetadataSetupFailed.class)),
    FREEIPA_INSTALL_FINISHED_EVENT(EventSelectorUtil.selector(InstallFreeIpaServicesSuccess.class)),
    FREEIPA_INSTALL_FAILED_EVENT(EventSelectorUtil.selector(InstallFreeIpaServicesFailed.class)),
    FREEIPA_PROVISION_FAILED_EVENT("FREEIPA_PROVISION_FAILED_EVENT"),
    FREEIPA_PROVISION_FINISHED_EVENT("FREEIPA_PROVISION_FINISHED_EVENT"),
    FREEIPA_PROVISION_FAILURE_HANDLED_EVENT("FREEIPA_PROVISION_FAILURE_HANDLED_EVENT");

    private final String event;

    FreeIpaProvisionEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
