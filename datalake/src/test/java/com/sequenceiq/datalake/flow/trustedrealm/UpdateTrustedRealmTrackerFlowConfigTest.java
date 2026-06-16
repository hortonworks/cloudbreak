package com.sequenceiq.datalake.flow.trustedrealm;

import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerEvent.UPDATE_TRUSTED_REALM_EVENT;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerEvent.UPDATE_TRUSTED_REALM_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerEvent.UPDATE_TRUSTED_REALM_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerEvent.UPDATE_TRUSTED_REALM_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerEvent.UPDATE_TRUSTED_REALM_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerState.INIT_STATE;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerState.UPDATE_TRUSTED_REALM_FAILED_STATE;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerState.UPDATE_TRUSTED_REALM_SUCCESS_STATE;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerState.UPDATE_TRUSTED_REALM_WAITING_STATE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition;

class UpdateTrustedRealmTrackerFlowConfigTest {

    private List<TransitionKeeper> storedTransitions;

    private UpdateTrustedRealmTrackerFlowConfig underTest;

    @BeforeEach
    void setUp() {
        underTest = new UpdateTrustedRealmTrackerFlowConfig();
        int ordinal = 1;
        storedTransitions = new ArrayList<>();
        for (Transition<UpdateTrustedRealmTrackerState, UpdateTrustedRealmTrackerEvent> transition : underTest.getTransitions()) {
            storedTransitions.add(new TransitionKeeper(ordinal, transition.getSource(), transition.getTarget(),
                    transition.getEvent(), transition.getFailureEvent(), transition.getFailureState()));
            ordinal++;
        }
    }

    @Test
    void testFlowEdgeConfig() {
        assertEquals(INIT_STATE, underTest.getEdgeConfig().getInitState());
        assertEquals(FINAL_STATE, underTest.getEdgeConfig().getFinalState());
        assertEquals(UPDATE_TRUSTED_REALM_FAILED_STATE, underTest.getEdgeConfig().getDefaultFailureState());
        assertEquals(UPDATE_TRUSTED_REALM_FAIL_HANDLED_EVENT, underTest.getEdgeConfig().getFailureHandled());
    }

    @Test
    void testInitEvents() {
        assertArrayEquals(new UpdateTrustedRealmTrackerEvent[]{UPDATE_TRUSTED_REALM_EVENT}, underTest.getInitEvents());
    }

    @Test
    void testDisplayName() {
        assertEquals("Update trusted realm", underTest.getDisplayName());
    }

    @Test
    void testGetEventsMatchesEnum() {
        assertEquals(UpdateTrustedRealmTrackerEvent.values().length, underTest.getEvents().length);
    }

    @MethodSource("scenarios")
    @ParameterizedTest(name = "{0}")
    void testTransitions(String testName, int expectedOrdinal, UpdateTrustedRealmTrackerState expectedFromState,
            UpdateTrustedRealmTrackerState expectedToState, UpdateTrustedRealmTrackerEvent expectedInitiatedEvent,
            UpdateTrustedRealmTrackerEvent expectedFailureEvent, UpdateTrustedRealmTrackerState expectedFailureState) {
        List<TransitionKeeper> selected = storedTransitions.stream()
                .filter(t -> t.getFromState() == expectedFromState)
                .collect(Collectors.toList());
        TransitionKeeper transition = selected.size() == 1 ? selected.get(0)
                : selected.stream().filter(t -> t.getOrdinal() == expectedOrdinal).findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to find transition for: " + testName));
        assertEquals(expectedOrdinal, transition.getOrdinal());
        assertEquals(expectedInitiatedEvent, transition.getInitiatedEvent());
        assertEquals(expectedFailureEvent, transition.getFailureEvent());
        assertEquals(expectedFailureState, transition.getFailureState());
        assertEquals(expectedToState, transition.getToState());
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][] {
                { "Init moves to waiting state",       1, INIT_STATE,                          UPDATE_TRUSTED_REALM_WAITING_STATE, UPDATE_TRUSTED_REALM_EVENT,         null,                            null },
                { "Waiting moves to success state",    2, UPDATE_TRUSTED_REALM_WAITING_STATE,  UPDATE_TRUSTED_REALM_SUCCESS_STATE,  UPDATE_TRUSTED_REALM_SUCCESS_EVENT, UPDATE_TRUSTED_REALM_FAILED_EVENT, null },
                { "Success moves to final state",      3, UPDATE_TRUSTED_REALM_SUCCESS_STATE,  FINAL_STATE,                         UPDATE_TRUSTED_REALM_FINISHED_EVENT, UPDATE_TRUSTED_REALM_FAILED_EVENT, null },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    private static final class TransitionKeeper {

        private final int ordinal;

        private final UpdateTrustedRealmTrackerState fromState;

        private final UpdateTrustedRealmTrackerState toState;

        private final UpdateTrustedRealmTrackerEvent initiatedEvent;

        private final UpdateTrustedRealmTrackerEvent failureEvent;

        private final UpdateTrustedRealmTrackerState failureState;

        TransitionKeeper(int ordinal, UpdateTrustedRealmTrackerState fromState, UpdateTrustedRealmTrackerState toState,
                UpdateTrustedRealmTrackerEvent initiatedEvent, UpdateTrustedRealmTrackerEvent failureEvent,
                UpdateTrustedRealmTrackerState failureState) {
            this.ordinal = ordinal;
            this.fromState = fromState;
            this.toState = toState;
            this.initiatedEvent = initiatedEvent;
            this.failureEvent = failureEvent;
            this.failureState = failureState;
        }

        int getOrdinal() {
            return ordinal;
        }

        UpdateTrustedRealmTrackerState getFromState() {
            return fromState;
        }

        UpdateTrustedRealmTrackerState getToState() {
            return toState;
        }

        UpdateTrustedRealmTrackerEvent getInitiatedEvent() {
            return initiatedEvent;
        }

        UpdateTrustedRealmTrackerEvent getFailureEvent() {
            return failureEvent;
        }

        UpdateTrustedRealmTrackerState getFailureState() {
            return failureState;
        }
    }
}
