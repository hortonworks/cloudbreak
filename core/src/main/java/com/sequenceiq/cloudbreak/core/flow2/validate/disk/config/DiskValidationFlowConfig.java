package com.sequenceiq.cloudbreak.core.flow2.validate.disk.config;

import static com.sequenceiq.cloudbreak.core.flow2.validate.disk.config.DiskValidationEvent.DISK_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.disk.config.DiskValidationEvent.DISK_VALIDATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.disk.config.DiskValidationEvent.DISK_VALIDATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.disk.config.DiskValidationEvent.DISK_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.disk.config.DiskValidationState.DISK_VALIDATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.disk.config.DiskValidationState.DISK_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.disk.config.DiskValidationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.disk.config.DiskValidationState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DiskValidationFlowConfig extends StackStatusFinalizerAbstractFlowConfig<DiskValidationState, DiskValidationEvent>
        implements RetryableFlowConfiguration<DiskValidationEvent> {

    private static final List<Transition<DiskValidationState, DiskValidationEvent>> TRANSITIONS =
            new Builder<DiskValidationState, DiskValidationEvent>()
            .defaultFailureEvent(DISK_VALIDATION_FAILED_EVENT)
            .from(INIT_STATE).to(DISK_VALIDATION_STATE).event(DISK_VALIDATION_EVENT).defaultFailureEvent()
            .from(DISK_VALIDATION_STATE).to(FINAL_STATE).event(DISK_VALIDATION_FINISHED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<DiskValidationState, DiskValidationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DISK_VALIDATION_FAILED_STATE, DISK_VALIDATION_FAILURE_HANDLED_EVENT);

    public DiskValidationFlowConfig() {
        super(DiskValidationState.class, DiskValidationEvent.class);
    }

    @Override
    protected List<Transition<DiskValidationState, DiskValidationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DiskValidationState, DiskValidationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DiskValidationEvent[] getEvents() {
        return DiskValidationEvent.values();
    }

    @Override
    public DiskValidationEvent[] getInitEvents() {
        return new DiskValidationEvent[] {
                DISK_VALIDATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Volume validation for stack";
    }

    @Override
    public DiskValidationEvent getRetryableEvent() {
        return DISK_VALIDATION_FAILURE_HANDLED_EVENT;
    }
}
