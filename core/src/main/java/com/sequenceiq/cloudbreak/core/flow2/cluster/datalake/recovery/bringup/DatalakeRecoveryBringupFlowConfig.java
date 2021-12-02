package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup.DatalakeRecoveryBringupEvent.RECOVERY_BRINGUP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup.DatalakeRecoveryBringupEvent.RECOVERY_BRINGUP_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup.DatalakeRecoveryBringupEvent.RECOVERY_BRINGUP_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup.DatalakeRecoveryBringupEvent.RECOVERY_BRINGUP_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup.DatalakeRecoveryBringupEvent.RECOVERY_BRINGUP_NEW_INSTANCES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup.DatalakeRecoveryBringupEvent.RECOVERY_RESTORE_COMPONENTS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup.DatalakeRecoveryBringupState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup.DatalakeRecoveryBringupState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup.DatalakeRecoveryBringupState.RECOVERY_BRINGUP_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup.DatalakeRecoveryBringupState.RECOVERY_BRINGUP_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup.DatalakeRecoveryBringupState.RECOVERY_RESTORE_COMPONENTS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup.DatalakeRecoveryBringupState.RECOVERY_SETUP_NEW_INSTANCES_STATE;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DatalakeRecoveryBringupFlowConfig extends AbstractFlowConfiguration<DatalakeRecoveryBringupState, DatalakeRecoveryBringupEvent>
    implements RetryableFlowConfiguration<DatalakeRecoveryBringupEvent> {

    private static final List<Transition<DatalakeRecoveryBringupState, DatalakeRecoveryBringupEvent>> TRANSITIONS =
        new Transition.Builder<DatalakeRecoveryBringupState, DatalakeRecoveryBringupEvent>()
                .defaultFailureEvent(RECOVERY_BRINGUP_FAILED_EVENT)

                .from(INIT_STATE).to(RECOVERY_RESTORE_COMPONENTS_STATE)
                .event(RECOVERY_BRINGUP_EVENT)
                .defaultFailureEvent()

                .from(RECOVERY_RESTORE_COMPONENTS_STATE).to(RECOVERY_SETUP_NEW_INSTANCES_STATE)
                .event(RECOVERY_RESTORE_COMPONENTS_FINISHED_EVENT)
                .defaultFailureEvent()

                .from(RECOVERY_SETUP_NEW_INSTANCES_STATE).to(RECOVERY_BRINGUP_FINISHED_STATE)
                .event(RECOVERY_BRINGUP_NEW_INSTANCES_FINISHED_EVENT)
                .defaultFailureEvent()

                .from(RECOVERY_BRINGUP_FINISHED_STATE).to(FINAL_STATE)
                .event(RECOVERY_BRINGUP_FINALIZED_EVENT)
                .defaultFailureEvent()

                .build();

    private static final FlowEdgeConfig<DatalakeRecoveryBringupState, DatalakeRecoveryBringupEvent> EDGE_CONFIG =
        new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, RECOVERY_BRINGUP_FAILED_STATE, RECOVERY_BRINGUP_FAIL_HANDLED_EVENT);

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

    public DatalakeRecoveryBringupFlowConfig() {
        super(DatalakeRecoveryBringupState.class, DatalakeRecoveryBringupEvent.class);
    }

    @Override
    protected List<Transition<DatalakeRecoveryBringupState, DatalakeRecoveryBringupEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<DatalakeRecoveryBringupState, DatalakeRecoveryBringupEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatalakeRecoveryBringupEvent[] getEvents() {
        return DatalakeRecoveryBringupEvent.values();
    }

    @Override
    public DatalakeRecoveryBringupEvent[] getInitEvents() {
        return new DatalakeRecoveryBringupEvent[]{
            RECOVERY_BRINGUP_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Bringing back SDX instances for recovery";
    }

    @Override
    public DatalakeRecoveryBringupEvent getRetryableEvent() {
        return RECOVERY_BRINGUP_FAILED_EVENT;
    }

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}
