package com.sequenceiq.freeipa.flow.freeipa.provision;

import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.BOOTSTRAP_MACHINES_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.BOOTSTRAP_MACHINES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_INSTALL_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_INSTALL_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_PROVISION_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_PROVISION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_PROVISION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_PROVISION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.HOST_METADATASETUP_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.HOST_METADATASETUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.BOOTSTRAPPING_MACHINES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.COLLECTING_HOST_METADATA_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.FREEIPA_INSTALL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.FREEIPA_PROVISION_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.FREEIPA_PROVISION_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class FreeIpaProvisionFlowConfig extends AbstractFlowConfiguration<FreeIpaProvisionState, FreeIpaProvisionEvent> {

    private static final FreeIpaProvisionEvent[] FREEIPA_INIT_EVENTS = {FREEIPA_PROVISION_EVENT};

    private static final FlowEdgeConfig<FreeIpaProvisionState, FreeIpaProvisionEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, FREEIPA_PROVISION_FAILED_STATE, FREEIPA_PROVISION_FAILURE_HANDLED_EVENT);

    private static final List<Transition<FreeIpaProvisionState, FreeIpaProvisionEvent>> TRANSITIONS =
            new Builder<FreeIpaProvisionState, FreeIpaProvisionEvent>().defaultFailureEvent(FREEIPA_PROVISION_FAILED_EVENT)
            .from(INIT_STATE).to(BOOTSTRAPPING_MACHINES_STATE).event(FREEIPA_PROVISION_EVENT).noFailureEvent()
            .from(BOOTSTRAPPING_MACHINES_STATE).to(COLLECTING_HOST_METADATA_STATE).event(BOOTSTRAP_MACHINES_FINISHED_EVENT)
                    .failureEvent(BOOTSTRAP_MACHINES_FAILED_EVENT)
            .from(COLLECTING_HOST_METADATA_STATE).to(FREEIPA_INSTALL_STATE).event(HOST_METADATASETUP_FINISHED_EVENT)
                    .failureEvent(HOST_METADATASETUP_FAILED_EVENT)
            .from(FREEIPA_INSTALL_STATE).to(FREEIPA_PROVISION_FINISHED_STATE).event(FREEIPA_INSTALL_FINISHED_EVENT).failureEvent(FREEIPA_INSTALL_FAILED_EVENT)
            .from(FREEIPA_PROVISION_FINISHED_STATE).to(FINAL_STATE).event(FREEIPA_PROVISION_FINISHED_EVENT).defaultFailureEvent()
            .build();

    public FreeIpaProvisionFlowConfig() {
        super(FreeIpaProvisionState.class, FreeIpaProvisionEvent.class);
    }

    @Override
    protected List<Transition<FreeIpaProvisionState, FreeIpaProvisionEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<FreeIpaProvisionState, FreeIpaProvisionEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIpaProvisionEvent[] getEvents() {
        return FreeIpaProvisionEvent.values();
    }

    @Override
    public FreeIpaProvisionEvent[] getInitEvents() {
        return FREEIPA_INIT_EVENTS;
    }
}
