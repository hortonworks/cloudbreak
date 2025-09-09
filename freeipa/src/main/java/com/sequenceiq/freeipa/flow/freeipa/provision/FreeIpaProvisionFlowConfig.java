package com.sequenceiq.freeipa.flow.freeipa.provision;

import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.BOOTSTRAP_MACHINES_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.BOOTSTRAP_MACHINES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.CLUSTER_PROXY_UPDATE_REGISTRATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.CLUSTER_PROXY_UPDATE_REGISTRATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_INSTALL_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_INSTALL_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_POST_INSTALL_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_POST_INSTALL_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_PROVISION_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_PROVISION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_PROVISION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.FREEIPA_PROVISION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.HOST_METADATASETUP_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.HOST_METADATASETUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.ORCHESTRATOR_CONFIG_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.ORCHESTRATOR_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.VALIDATING_CLOUD_STORAGE_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent.VALIDATING_CLOUD_STORAGE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.BOOTSTRAPPING_MACHINES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.CLUSTERPROXY_UPDATE_REGISTRATION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.COLLECTING_HOST_METADATA_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.FREEIPA_INSTALL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.FREEIPA_POST_INSTALL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.FREEIPA_PROVISION_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.FREEIPA_PROVISION_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.ORCHESTRATOR_CONFIG_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState.VALIDATING_CLOUD_STORAGE_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class FreeIpaProvisionFlowConfig extends StackStatusFinalizerAbstractFlowConfig<FreeIpaProvisionState, FreeIpaProvisionEvent>
        implements RetryableFlowConfiguration<FreeIpaProvisionEvent> {

    private static final FreeIpaProvisionEvent[] FREEIPA_INIT_EVENTS = {FREEIPA_PROVISION_EVENT};

    private static final FlowEdgeConfig<FreeIpaProvisionState, FreeIpaProvisionEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, FREEIPA_PROVISION_FAILED_STATE, FREEIPA_PROVISION_FAILURE_HANDLED_EVENT);

    private static final List<Transition<FreeIpaProvisionState, FreeIpaProvisionEvent>> TRANSITIONS =
            new Builder<FreeIpaProvisionState, FreeIpaProvisionEvent>().defaultFailureEvent(FREEIPA_PROVISION_FAILED_EVENT)
            .from(INIT_STATE).to(BOOTSTRAPPING_MACHINES_STATE).event(FREEIPA_PROVISION_EVENT).noFailureEvent()
            .from(BOOTSTRAPPING_MACHINES_STATE).to(COLLECTING_HOST_METADATA_STATE).event(BOOTSTRAP_MACHINES_FINISHED_EVENT)
                    .failureEvent(BOOTSTRAP_MACHINES_FAILED_EVENT)
            .from(COLLECTING_HOST_METADATA_STATE).to(ORCHESTRATOR_CONFIG_STATE).event(HOST_METADATASETUP_FINISHED_EVENT)
                    .failureEvent(HOST_METADATASETUP_FAILED_EVENT)
            .from(ORCHESTRATOR_CONFIG_STATE).to(VALIDATING_CLOUD_STORAGE_STATE).event(ORCHESTRATOR_CONFIG_FINISHED_EVENT)
                    .failureEvent(ORCHESTRATOR_CONFIG_FAILED_EVENT)
            .from(VALIDATING_CLOUD_STORAGE_STATE).to(FREEIPA_INSTALL_STATE).event(VALIDATING_CLOUD_STORAGE_FINISHED_EVENT)
                    .failureEvent(VALIDATING_CLOUD_STORAGE_FAILED_EVENT)
            .from(FREEIPA_INSTALL_STATE).to(CLUSTERPROXY_UPDATE_REGISTRATION_STATE)
                    .event(FREEIPA_INSTALL_FINISHED_EVENT).failureEvent(FREEIPA_INSTALL_FAILED_EVENT)
            .from(CLUSTERPROXY_UPDATE_REGISTRATION_STATE).to(FREEIPA_POST_INSTALL_STATE)
                    .event(CLUSTER_PROXY_UPDATE_REGISTRATION_FINISHED_EVENT).failureEvent(CLUSTER_PROXY_UPDATE_REGISTRATION_FAILED_EVENT)
            .from(FREEIPA_POST_INSTALL_STATE).to(FREEIPA_PROVISION_FINISHED_STATE).event(FREEIPA_POST_INSTALL_FINISHED_EVENT)
                    .failureEvent(FREEIPA_POST_INSTALL_FAILED_EVENT)
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
    public FlowEdgeConfig<FreeIpaProvisionState, FreeIpaProvisionEvent> getEdgeConfig() {
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

    @Override
    public String getDisplayName() {
        return "Provision FreeIPA";
    }

    @Override
    public FreeIpaProvisionEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }
}
