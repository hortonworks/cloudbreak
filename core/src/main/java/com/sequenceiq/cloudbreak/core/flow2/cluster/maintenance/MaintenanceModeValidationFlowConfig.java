package com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.FETCH_STACK_REPO_INFO_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.START_VALIDATION_FLOW_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATE_AMBARI_REPO_INFO_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATE_IMAGE_COMPATIBILITY_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATE_STACK_REPO_INFO_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATION_FLOW_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent.VALIDATION_FLOW_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationState.FETCH_STACK_REPO_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationState.VALIDATE_AMBARI_REPO_INFO_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationState.VALIDATE_IMAGE_COMPATIBILITY_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationState.VALIDATE_STACK_REPO_INFO_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationState.VALIDATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationState.VALIDATION_FINISHED_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class MaintenanceModeValidationFlowConfig extends AbstractFlowConfiguration<MaintenanceModeValidationState, MaintenanceModeValidationEvent> {
    private static final List<Transition<MaintenanceModeValidationState, MaintenanceModeValidationEvent>> TRANSITIONS = new
            Builder<MaintenanceModeValidationState, MaintenanceModeValidationEvent>()
            .defaultFailureEvent(VALIDATION_FLOW_FAILED_EVENT)
            .from(INIT_STATE).to(FETCH_STACK_REPO_STATE).event(START_VALIDATION_FLOW_EVENT).noFailureEvent()
            .from(FETCH_STACK_REPO_STATE).to(VALIDATE_STACK_REPO_INFO_STATE).event(FETCH_STACK_REPO_INFO_FINISHED_EVENT).defaultFailureEvent()
            .from(VALIDATE_STACK_REPO_INFO_STATE).to(VALIDATE_AMBARI_REPO_INFO_STATE).event(VALIDATE_STACK_REPO_INFO_FINISHED_EVENT).defaultFailureEvent()
            .from(VALIDATE_AMBARI_REPO_INFO_STATE).to(VALIDATE_IMAGE_COMPATIBILITY_STATE).event(VALIDATE_AMBARI_REPO_INFO_FINISHED_EVENT).defaultFailureEvent()
            .from(VALIDATE_IMAGE_COMPATIBILITY_STATE).to(VALIDATION_FINISHED_STATE).event(VALIDATE_IMAGE_COMPATIBILITY_FINISHED_EVENT).defaultFailureEvent()
            .from(VALIDATION_FINISHED_STATE).to(FINAL_STATE).event(VALIDATION_FLOW_FINISHED_EVENT).noFailureEvent()
            .build();

    private static final FlowEdgeConfig<MaintenanceModeValidationState, MaintenanceModeValidationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, VALIDATION_FAILED_STATE, VALIDATION_FAIL_HANDLED_EVENT);

    public MaintenanceModeValidationFlowConfig() {
        super(MaintenanceModeValidationState.class, MaintenanceModeValidationEvent.class);
    }

    @Override
    public MaintenanceModeValidationEvent[] getEvents() {
        return MaintenanceModeValidationEvent.values();
    }

    @Override
    public MaintenanceModeValidationEvent[] getInitEvents() {
        return new MaintenanceModeValidationEvent[] {
                START_VALIDATION_FLOW_EVENT
        };
    }

    @Override
    protected List<Transition<MaintenanceModeValidationState, MaintenanceModeValidationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<MaintenanceModeValidationState, MaintenanceModeValidationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
