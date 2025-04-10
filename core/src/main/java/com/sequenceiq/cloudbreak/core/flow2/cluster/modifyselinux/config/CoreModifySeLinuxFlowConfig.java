package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.CoreModifySeLinuxState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.CoreModifySeLinuxState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.CoreModifySeLinuxState.MODIFY_SELINUX_CORE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.CoreModifySeLinuxState.MODIFY_SELINUX_CORE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.CoreModifySeLinuxState.MODIFY_SELINUX_CORE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.CoreModifySeLinuxState.MODIFY_SELINUX_CORE_VALIDATION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.CoreModifySeLinuxState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxStateSelectors;

@Component
public class CoreModifySeLinuxFlowConfig extends StackStatusFinalizerAbstractFlowConfig<CoreModifySeLinuxState, CoreModifySeLinuxStateSelectors> {

    private static final List<Transition<CoreModifySeLinuxState, CoreModifySeLinuxStateSelectors>> TRANSITIONS =
            new Transition.Builder<CoreModifySeLinuxState, CoreModifySeLinuxStateSelectors>()
            .defaultFailureEvent(CoreModifySeLinuxStateSelectors.FAILED_MODIFY_SELINUX_CORE_EVENT)

            .from(INIT_STATE)
            .to(MODIFY_SELINUX_CORE_VALIDATION_STATE)
            .event(CoreModifySeLinuxStateSelectors.CORE_MODIFY_SELINUX_EVENT)
            .defaultFailureEvent()

            .from(MODIFY_SELINUX_CORE_VALIDATION_STATE)
            .to(MODIFY_SELINUX_CORE_STATE)
            .event(CoreModifySeLinuxStateSelectors.MODIFY_SELINUX_CORE_EVENT)
            .defaultFailureEvent()

            .from(MODIFY_SELINUX_CORE_STATE)
            .to(MODIFY_SELINUX_CORE_FINISHED_STATE)
            .event(CoreModifySeLinuxStateSelectors.FINISH_MODIFY_SELINUX_CORE_EVENT)
            .defaultFailureEvent()

            .from(MODIFY_SELINUX_CORE_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(CoreModifySeLinuxStateSelectors.FINALIZE_MODIFY_SELINUX_CORE_EVENT)
            .defaultFailureEvent()

            .build();

    protected CoreModifySeLinuxFlowConfig() {
        super(CoreModifySeLinuxState.class, CoreModifySeLinuxStateSelectors.class);
    }

    @Override
    protected List<Transition<CoreModifySeLinuxState, CoreModifySeLinuxStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<CoreModifySeLinuxState, CoreModifySeLinuxStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, MODIFY_SELINUX_CORE_FAILED_STATE,
                CoreModifySeLinuxStateSelectors.HANDLED_FAILED_MODIFY_SELINUX_CORE_EVENT);
    }

    @Override
    public CoreModifySeLinuxStateSelectors[] getEvents() {
        return CoreModifySeLinuxStateSelectors.values();
    }

    @Override
    public CoreModifySeLinuxStateSelectors[] getInitEvents() {
        return new CoreModifySeLinuxStateSelectors[] {CoreModifySeLinuxStateSelectors.CORE_MODIFY_SELINUX_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Enable SeLinux Core";
    }

}
