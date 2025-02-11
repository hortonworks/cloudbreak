package com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.CoreEnableSeLinuxState.ENABLE_SELINUX_CORE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.CoreEnableSeLinuxState.ENABLE_SELINUX_CORE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.CoreEnableSeLinuxState.ENABLE_SELINUX_CORE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.CoreEnableSeLinuxState.ENABLE_SELINUX_CORE_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.CoreEnableSeLinuxState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.CoreEnableSeLinuxState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors.ENABLE_SELINUX_CORE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors.ENABLE_SELINUX_CORE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors.FAILED_ENABLE_SELINUX_CORE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors.FINALIZE_ENABLE_SELINUX_CORE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors.FINISH_ENABLE_SELINUX_CORE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors.HANDLED_FAILED_ENABLE_SELINUX_CORE_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.CoreEnableSeLinuxState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors;

@Component
public class CoreEnableSeLinuxFlowConfig extends StackStatusFinalizerAbstractFlowConfig<CoreEnableSeLinuxState, CoreEnableSeLinuxStateSelectors> {

    private static final List<Transition<CoreEnableSeLinuxState, CoreEnableSeLinuxStateSelectors>> TRANSITIONS =
            new Transition.Builder<CoreEnableSeLinuxState, CoreEnableSeLinuxStateSelectors>()
            .defaultFailureEvent(FAILED_ENABLE_SELINUX_CORE_EVENT)

            .from(INIT_STATE)
            .to(ENABLE_SELINUX_CORE_VALIDATION_STATE)
            .event(ENABLE_SELINUX_CORE_VALIDATION_EVENT)
            .defaultFailureEvent()

            .from(ENABLE_SELINUX_CORE_VALIDATION_STATE)
            .to(ENABLE_SELINUX_CORE_STATE)
            .event(ENABLE_SELINUX_CORE_EVENT)
            .defaultFailureEvent()

            .from(ENABLE_SELINUX_CORE_STATE)
            .to(ENABLE_SELINUX_CORE_FINISHED_STATE)
            .event(FINISH_ENABLE_SELINUX_CORE_EVENT)
            .defaultFailureEvent()

            .from(ENABLE_SELINUX_CORE_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(FINALIZE_ENABLE_SELINUX_CORE_EVENT)
            .defaultFailureEvent()

            .build();

    protected CoreEnableSeLinuxFlowConfig() {
        super(CoreEnableSeLinuxState.class, CoreEnableSeLinuxStateSelectors.class);
    }

    @Override
    protected List<Transition<CoreEnableSeLinuxState, CoreEnableSeLinuxStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<CoreEnableSeLinuxState, CoreEnableSeLinuxStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, ENABLE_SELINUX_CORE_FAILED_STATE, HANDLED_FAILED_ENABLE_SELINUX_CORE_EVENT);
    }

    @Override
    public CoreEnableSeLinuxStateSelectors[] getEvents() {
        return CoreEnableSeLinuxStateSelectors.values();
    }

    @Override
    public CoreEnableSeLinuxStateSelectors[] getInitEvents() {
        return new CoreEnableSeLinuxStateSelectors[] {ENABLE_SELINUX_CORE_VALIDATION_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Enable SeLinux Core";
    }

}
