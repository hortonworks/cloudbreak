package com.sequenceiq.freeipa.flow.freeipa.upscale;

import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_ADD_INSTANCES_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_ADD_INSTANCES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_BOOTSTRAP_MACHINES_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_BOOTSTRAP_MACHINES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_DISABLE_STATUS_CHECKER_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_DISABLE_STATUS_CHECKER_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_ENABLE_STATUS_CHECKER_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_ENABLE_STATUS_CHECKER_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_EXTEND_METADATA_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_EXTEND_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_FREEIPA_INSTALL_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_FREEIPA_INSTALL_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_FREEIPA_POST_INSTALL_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_FREEIPA_POST_INSTALL_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_HOST_METADATASETUP_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_HOST_METADATASETUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_RECORD_HOSTNAMES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_SAVE_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_STARTING_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_TLS_SETUP_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_TLS_SETUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_UPDATE_CLUSTER_PROXY_REGISTRATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_UPDATE_CLUSTER_PROXY_REGISTRATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_UPDATE_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_VALIDATE_INSTANCES_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_VALIDATE_INSTANCES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_ADD_INSTANCES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_BOOTSTRAPPING_MACHINES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_DISABLE_STATUS_CHECKER_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_ENABLE_STATUS_CHECKER_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_FREEIPA_POST_INSTALL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_RECORD_HOSTNAMES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_UPDATE_CLUSTERPROXY_REGISTRATION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_COLLECTING_HOST_METADATA_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_EXTEND_METADATA_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_FAIL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_FREEIPA_INSTALL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_SAVE_METADATA_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_STARTING_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_TLS_SETUP_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_UPDATE_METADATA_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState.UPSCALE_VALIDATE_INSTANCES_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class UpscaleFlowConfig extends AbstractFlowConfiguration<UpscaleState, UpscaleFlowEvent> {
    private static final List<Transition<UpscaleState, UpscaleFlowEvent>> TRANSITIONS =
            new Transition.Builder<UpscaleState, UpscaleFlowEvent>()
                    .defaultFailureEvent(FAILURE_EVENT)

                    .from(INIT_STATE).to(UPSCALE_STARTING_STATE)
                    .event(UPSCALE_EVENT)
                    .defaultFailureEvent()

                    .from(UPSCALE_STARTING_STATE).to(UPSCALE_DISABLE_STATUS_CHECKER_STATE)
                    .event(UPSCALE_STARTING_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPSCALE_DISABLE_STATUS_CHECKER_STATE).to(UPSCALE_ADD_INSTANCES_STATE)
                    .event(UPSCALE_DISABLE_STATUS_CHECKER_FINISHED_EVENT)
                    .failureEvent(UPSCALE_DISABLE_STATUS_CHECKER_FAILED_EVENT)

                    .from(UPSCALE_ADD_INSTANCES_STATE).to(UPSCALE_VALIDATE_INSTANCES_STATE)
                    .event(UPSCALE_ADD_INSTANCES_FINISHED_EVENT)
                    .failureEvent(UPSCALE_ADD_INSTANCES_FAILED_EVENT)

                    .from(UPSCALE_VALIDATE_INSTANCES_STATE).to(UPSCALE_EXTEND_METADATA_STATE)
                    .event(UPSCALE_VALIDATE_INSTANCES_FINISHED_EVENT)
                    .failureEvent(UPSCALE_VALIDATE_INSTANCES_FAILED_EVENT)

                    .from(UPSCALE_EXTEND_METADATA_STATE).to(UPSCALE_SAVE_METADATA_STATE)
                    .event(UPSCALE_EXTEND_METADATA_FINISHED_EVENT)
                    .failureEvent(UPSCALE_EXTEND_METADATA_FAILED_EVENT)

                    .from(UPSCALE_SAVE_METADATA_STATE).to(UPSCALE_TLS_SETUP_STATE)
                    .event(UPSCALE_SAVE_METADATA_FINISHED_EVENT)
                    .failureEvent(UPSCALE_TLS_SETUP_FAILED_EVENT)

                    .from(UPSCALE_TLS_SETUP_STATE).to(UPSCALE_BOOTSTRAPPING_MACHINES_STATE)
                    .event(UPSCALE_TLS_SETUP_FINISHED_EVENT)
                    .failureEvent(UPSCALE_TLS_SETUP_FAILED_EVENT)

                    .from(UPSCALE_BOOTSTRAPPING_MACHINES_STATE).to(UPSCALE_COLLECTING_HOST_METADATA_STATE)
                    .event(UPSCALE_BOOTSTRAP_MACHINES_FINISHED_EVENT)
                    .failureEvent(UPSCALE_BOOTSTRAP_MACHINES_FAILED_EVENT)

                    .from(UPSCALE_COLLECTING_HOST_METADATA_STATE).to(UPSCALE_RECORD_HOSTNAMES_STATE)
                    .event(UPSCALE_HOST_METADATASETUP_FINISHED_EVENT)
                    .failureEvent(UPSCALE_HOST_METADATASETUP_FAILED_EVENT)

                    .from(UPSCALE_RECORD_HOSTNAMES_STATE).to(UPSCALE_FREEIPA_INSTALL_STATE)
                    .event(UPSCALE_RECORD_HOSTNAMES_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPSCALE_FREEIPA_INSTALL_STATE).to(UPSCALE_UPDATE_CLUSTERPROXY_REGISTRATION_STATE)
                    .event(UPSCALE_FREEIPA_INSTALL_FINISHED_EVENT)
                    .failureEvent(UPSCALE_FREEIPA_INSTALL_FAILED_EVENT)

                    .from(UPSCALE_UPDATE_CLUSTERPROXY_REGISTRATION_STATE).to(UPSCALE_FREEIPA_POST_INSTALL_STATE)
                    .event(UPSCALE_UPDATE_CLUSTER_PROXY_REGISTRATION_FINISHED_EVENT)
                    .failureEvent(UPSCALE_UPDATE_CLUSTER_PROXY_REGISTRATION_FAILED_EVENT)

                    .from(UPSCALE_FREEIPA_POST_INSTALL_STATE).to(UPSCALE_UPDATE_METADATA_STATE)
                    .event(UPSCALE_FREEIPA_POST_INSTALL_FINISHED_EVENT)
                    .failureEvent(UPSCALE_FREEIPA_POST_INSTALL_FAILED_EVENT)

                    .from(UPSCALE_UPDATE_METADATA_STATE).to(UPSCALE_ENABLE_STATUS_CHECKER_STATE)
                    .event(UPSCALE_UPDATE_METADATA_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPSCALE_ENABLE_STATUS_CHECKER_STATE).to(UPSCALE_FINISHED_STATE)
                    .event(UPSCALE_ENABLE_STATUS_CHECKER_FINISHED_EVENT)
                    .failureEvent(UPSCALE_ENABLE_STATUS_CHECKER_FAILED_EVENT)

                    .from(UPSCALE_FINISHED_STATE).to(FINAL_STATE)
                    .event(UPSCALE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<UpscaleState, UpscaleFlowEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            UPSCALE_FAIL_STATE, FAIL_HANDLED_EVENT);

    public UpscaleFlowConfig() {
        super(UpscaleState.class, UpscaleFlowEvent.class);
    }

    @Override
    protected List<Transition<UpscaleState, UpscaleFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<UpscaleState, UpscaleFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public UpscaleFlowEvent[] getEvents() {
        return UpscaleFlowEvent.values();
    }

    @Override
    public UpscaleFlowEvent[] getInitEvents() {
        return new UpscaleFlowEvent[] { UPSCALE_EVENT };
    }

    @Override
    public String getDisplayName() {
        return "Upscale FreeIPA";
    }
}
