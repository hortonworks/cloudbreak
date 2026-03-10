package com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.ResetJvmParamsFlowEvent.RESET_JVM_PARAMS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.ResetJvmParamsFlowEvent.RESET_JVM_PARAMS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.ResetJvmParamsFlowEvent.RESET_JVM_PARAMS_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.ResetJvmParamsFlowEvent.RESET_JVM_PARAMS_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.ResetJvmParamsFlowEvent.RESET_JVM_PARAMS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.ResetJvmParamsFlowState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.ResetJvmParamsFlowState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.ResetJvmParamsFlowState.RESET_JVM_PARAMS_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.ResetJvmParamsFlowState.RESET_JVM_PARAMS_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.ResetJvmParamsFlowState.RESET_JVM_PARAMS_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class ResetJvmParamsFlowConfig extends StackStatusFinalizerAbstractFlowConfig<ResetJvmParamsFlowState, ResetJvmParamsFlowEvent> {

    public static final FlowEdgeConfig<ResetJvmParamsFlowState, ResetJvmParamsFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, RESET_JVM_PARAMS_FAILED_STATE, RESET_JVM_PARAMS_FAIL_HANDLED_EVENT);

    private static final List<Transition<ResetJvmParamsFlowState, ResetJvmParamsFlowEvent>> TRANSITIONS =
            new Builder<ResetJvmParamsFlowState, ResetJvmParamsFlowEvent>()
                    .defaultFailureEvent(RESET_JVM_PARAMS_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(RESET_JVM_PARAMS_STATE)
                    .event(RESET_JVM_PARAMS_EVENT)
                    .defaultFailureEvent()

                    .from(RESET_JVM_PARAMS_STATE)
                    .to(RESET_JVM_PARAMS_FINISHED_STATE)
                    .event(RESET_JVM_PARAMS_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(RESET_JVM_PARAMS_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(RESET_JVM_PARAMS_FINALIZED_EVENT)
                    .defaultFailureEvent()
                    .build();

    protected ResetJvmParamsFlowConfig() {
        super(ResetJvmParamsFlowState.class, ResetJvmParamsFlowEvent.class);
    }

    @Override
    protected List<Transition<ResetJvmParamsFlowState, ResetJvmParamsFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ResetJvmParamsFlowState, ResetJvmParamsFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ResetJvmParamsFlowEvent[] getEvents() {
        return ResetJvmParamsFlowEvent.values();
    }

    @Override
    public ResetJvmParamsFlowEvent[] getInitEvents() {
        return new ResetJvmParamsFlowEvent[]{
                RESET_JVM_PARAMS_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Reset JVM params";
    }
}
