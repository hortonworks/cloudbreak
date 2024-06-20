package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.ATTACHED_VOLUME_CONSUMPTION_COLLECTION_UNSCHEDULING_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.CCM_KEY_DEREGISTER_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.CLUSTER_PROXY_DEREGISTER_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.DELETE_USERDATA_SECRETS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.PRE_TERMINATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.TERMINATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.TERMINATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.TERMINATION_STATE;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.flow.core.RestartAction;

class StackTerminationStateTest {

    private static final Map<StackTerminationState, String> ENUM_TO_ACTION_MAP = new EnumMap<>(Map.ofEntries(
            entry(PRE_TERMINATION_STATE, "com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackPreTerminationAction"),
            entry(DELETE_USERDATA_SECRETS_STATE, "com.sequenceiq.cloudbreak.core.flow2.stack.termination.DeleteUserdataSecretsAction"),
            entry(CLUSTER_PROXY_DEREGISTER_STATE, "com.sequenceiq.cloudbreak.core.flow2.stack.termination.ClusterProxyDeregisterAction"),
            entry(CCM_KEY_DEREGISTER_STATE, "com.sequenceiq.cloudbreak.core.flow2.stack.termination.CcmKeyDeregisterAction"),
            entry(ATTACHED_VOLUME_CONSUMPTION_COLLECTION_UNSCHEDULING_STATE,
                    "com.sequenceiq.cloudbreak.core.flow2.stack.termination.AttachedVolumeConsumptionCollectionUnschedulingAction"),
            entry(TERMINATION_STATE, "com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationAction"),
            entry(TERMINATION_FAILED_STATE, "com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFailureAction"),
            entry(TERMINATION_FINISHED_STATE, "com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFinishedAction")
    ));

    @ParameterizedTest(name = "underTest={0}")
    @EnumSource(StackTerminationState.class)
    void actionTest(StackTerminationState underTest) {
        Class<? extends AbstractStackAction<?, ?, ?, ?>> action = underTest.action();

        assertThat(action == null ? null : action.getName()).isEqualTo(ENUM_TO_ACTION_MAP.get(underTest));
    }

    @ParameterizedTest(name = "underTest={0}")
    @EnumSource(StackTerminationState.class)
    void restartActionTest(StackTerminationState underTest) {
        Class<? extends RestartAction> restartAction = underTest.restartAction();

        assertThat(restartAction).isNotNull();
        assertThat(restartAction.getName()).isEqualTo("com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction");
    }

}