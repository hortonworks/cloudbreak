package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw;

import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_SWITCH_FREEIPA_MASTER_TO_PRIMARY_GATEWAY_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState.CHANGE_PRIMARY_GATEWAY_FAIL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState.CHANGE_PRIMARY_GATEWAY_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState.CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState.CHANGE_PRIMARY_SWITCH_FREEIPA_MASTER_TO_PRIMARY_GATEWAY_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState.INIT_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition;

class ChangePrimaryGatewayFlowConfigTest {

    private final ChangePrimaryGatewayFlowConfig underTest = new ChangePrimaryGatewayFlowConfig();

    @Test
    void switchMasterTransitionsIntoVerifyReplication() {
        Transition<ChangePrimaryGatewayState, ChangePrimaryGatewayFlowEvent> transition =
                findTransition(CHANGE_PRIMARY_SWITCH_FREEIPA_MASTER_TO_PRIMARY_GATEWAY_STATE);

        assertEquals(CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_STATE, transition.getTarget());
        assertEquals(CHANGE_PRIMARY_GATEWAY_SWITCH_FREEIPA_MASTER_TO_PRIMARY_GATEWAY_FINISHED_EVENT, transition.getEvent());
    }

    @Test
    void verifyReplicationTransitionsIntoFinished() {
        Transition<ChangePrimaryGatewayState, ChangePrimaryGatewayFlowEvent> transition =
                findTransition(CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_STATE);

        assertEquals(CHANGE_PRIMARY_GATEWAY_FINISHED_STATE, transition.getTarget());
        assertEquals(CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_FINISHED_EVENT, transition.getEvent());
    }

    @Test
    void verifyReplicationHasDedicatedFailureEvent() {
        Transition<ChangePrimaryGatewayState, ChangePrimaryGatewayFlowEvent> transition =
                findTransition(CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_STATE);

        assertEquals(CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_FAILED_EVENT, transition.getFailureEvent());
    }

    @Test
    void finishedStateIsReachedOnlyThroughVerifyReplication() {
        List<Transition<ChangePrimaryGatewayState, ChangePrimaryGatewayFlowEvent>> transitionsIntoFinished = underTest.getTransitions().stream()
                .filter(transition -> transition.getTarget() == CHANGE_PRIMARY_GATEWAY_FINISHED_STATE)
                .toList();

        assertEquals(1, transitionsIntoFinished.size());
        assertEquals(CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_STATE, transitionsIntoFinished.get(0).getSource());
    }

    @Test
    void everyStateExceptTerminalOnesIsATransitionSource() {
        Set<ChangePrimaryGatewayState> sourceStates = underTest.getTransitions().stream()
                .map(Transition::getSource)
                .collect(Collectors.toSet());

        Set<ChangePrimaryGatewayState> expectedSources = Arrays.stream(ChangePrimaryGatewayState.values())
                .filter(state -> state != FINAL_STATE && state != CHANGE_PRIMARY_GATEWAY_FAIL_STATE)
                .collect(Collectors.toSet());

        assertEquals(expectedSources, sourceStates);
    }

    @Test
    void initEventsContainChangePrimaryGatewayEvent() {
        assertEquals(List.of(CHANGE_PRIMARY_GATEWAY_EVENT), Arrays.asList(underTest.getInitEvents()));
    }

    @Test
    void edgeConfigIsConsistent() {
        assertEquals(INIT_STATE, underTest.getEdgeConfig().getInitState());
        assertEquals(FINAL_STATE, underTest.getEdgeConfig().getFinalState());
        assertEquals(CHANGE_PRIMARY_GATEWAY_FAIL_STATE, underTest.getEdgeConfig().getDefaultFailureState());
    }

    @Test
    void finishedStateTransitionsIntoFinalState() {
        Transition<ChangePrimaryGatewayState, ChangePrimaryGatewayFlowEvent> transition = findTransition(CHANGE_PRIMARY_GATEWAY_FINISHED_STATE);

        assertEquals(FINAL_STATE, transition.getTarget());
        assertEquals(CHANGE_PRIMARY_GATEWAY_FINISHED_EVENT, transition.getEvent());
    }

    private Transition<ChangePrimaryGatewayState, ChangePrimaryGatewayFlowEvent> findTransition(ChangePrimaryGatewayState source) {
        return underTest.getTransitions().stream()
                .filter(transition -> transition.getSource() == source)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Transition from " + source + " is missing."));
    }
}
