package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.ATTACHED_VOLUME_CONSUMPTION_COLLECTION_UNSCHEDULING_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.ATTACHED_VOLUME_CONSUMPTION_COLLECTION_UNSCHEDULING_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.CCM_KEY_DEREGISTER_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.CCM_KEY_DEREGISTER_SUCCEEDED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.CLUSTER_PROXY_DEREGISTER_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.CLUSTER_PROXY_DEREGISTER_SUCCEEDED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.DELETE_USERDATA_SECRETS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.DELETE_USERDATA_SECRETS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.PRE_TERMINATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.PRE_TERMINATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.RECOVERY_TERMINATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.STACK_TERMINATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.TERMINATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.TERMINATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.TERMINATION_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.TERMINATION_FINISHED_EVENT;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class StackTerminationEventTest {

    private static final Map<StackTerminationEvent, String> ENUM_TO_EVENT_MAP = new EnumMap<>(Map.ofEntries(
            entry(TERMINATION_EVENT, "STACK_TERMINATE_TRIGGER_EVENT"), entry(RECOVERY_TERMINATION_EVENT, "STACK_RECOVERY_TERMINATION_EVENT"),
            entry(PRE_TERMINATION_FAILED_EVENT, "STACKPRETERMINATIONFAILED"), entry(PRE_TERMINATION_FINISHED_EVENT, "STACKPRETERMINATIONSUCCESS"),
            entry(DELETE_USERDATA_SECRETS_FINISHED_EVENT, "DELETEUSERDATASECRETSFINISHED"),
            entry(DELETE_USERDATA_SECRETS_FAILED_EVENT, "DELETEUSERDATASECRETSFAILED"),
            entry(CLUSTER_PROXY_DEREGISTER_SUCCEEDED_EVENT, "CLUSTERPROXYDEREGISTERSUCCESS"),
            entry(CLUSTER_PROXY_DEREGISTER_FAILED_EVENT, "CLUSTER_PROXY_DEREGISTER_FAILED_EVENT"),
            entry(CCM_KEY_DEREGISTER_SUCCEEDED_EVENT, "CCMKEYDEREGISTERSUCCESS"), entry(CCM_KEY_DEREGISTER_FAILED_EVENT, "CCM_KEY_DEREGISTER_FAILED_EVENT"),
            entry(ATTACHED_VOLUME_CONSUMPTION_COLLECTION_UNSCHEDULING_FINISHED_EVENT, "ATTACHEDVOLUMECONSUMPTIONCOLLECTIONUNSCHEDULINGSUCCESS"),
            entry(ATTACHED_VOLUME_CONSUMPTION_COLLECTION_UNSCHEDULING_FAILED_EVENT, "ATTACHEDVOLUMECONSUMPTIONCOLLECTIONUNSCHEDULINGFAILED"),
            entry(TERMINATION_FINISHED_EVENT, "TERMINATESTACKRESULT"), entry(TERMINATION_FAILED_EVENT, "TERMINATESTACKRESULT_ERROR"),
            entry(TERMINATION_FINALIZED_EVENT, "TERMINATESTACKFINALIZED"), entry(STACK_TERMINATION_FAIL_HANDLED_EVENT, "TERMINATIONFAILHANDLED")
    ));

    @ParameterizedTest(name = "underTest={0}")
    @EnumSource(StackTerminationEvent.class)
    void eventTest(StackTerminationEvent underTest) {
        assertThat(underTest.event()).isEqualTo(ENUM_TO_EVENT_MAP.get(underTest));
    }

}