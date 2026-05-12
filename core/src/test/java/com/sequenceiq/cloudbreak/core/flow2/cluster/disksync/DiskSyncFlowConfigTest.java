package com.sequenceiq.cloudbreak.core.flow2.cluster.disksync;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.DISK_SYNC_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.DISK_SYNC_PROCESS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.DISK_SYNC_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncState.DISK_SYNC_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncState.DISK_SYNC_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncState.DISK_SYNC_INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncState.INIT_STATE;
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

class DiskSyncFlowConfigTest {

    private List<TransitionKeeper> storedTransitions;

    private DiskSyncFlowConfig underTest;

    @BeforeEach
    void setUp() {
        underTest = new DiskSyncFlowConfig();
        int ordinal = 1;
        storedTransitions = new ArrayList<>();
        for (Transition<DiskSyncState, DiskSyncEvent> transition : underTest.getTransitions()) {
            storedTransitions.add(new TransitionKeeper(ordinal, transition.getSource(), transition.getTarget(),
                    transition.getEvent(), transition.getFailureEvent(), transition.getFailureState()));
            ordinal++;
        }
    }

    @Test
    void testFlowEdgeConfig() {
        assertEquals(INIT_STATE, underTest.getEdgeConfig().getInitState());
        assertEquals(FINAL_STATE, underTest.getEdgeConfig().getFinalState());
        assertEquals(DISK_SYNC_FAILED_STATE, underTest.getEdgeConfig().getDefaultFailureState());
        assertEquals(DISK_SYNC_FAILURE_HANDLED_EVENT, underTest.getEdgeConfig().getFailureHandled());
    }

    @Test
    void testInitEvents() {
        assertArrayEquals(new DiskSyncEvent[] { DISK_SYNC_TRIGGER_EVENT }, underTest.getInitEvents());
    }

    @Test
    void testRetryableEvent() {
        assertEquals(DISK_SYNC_FAILURE_HANDLED_EVENT, underTest.getRetryableEvent());
    }

    @Test
    void testDisplayName() {
        assertEquals("Synchronizing disk metadata on the stack", underTest.getDisplayName());
    }

    @Test
    void testGetEventsMatchesEnum() {
        assertEquals(DiskSyncEvent.values().length, underTest.getEvents().length);
    }

    @MethodSource("scenarios")
    @ParameterizedTest(name = "{0}")
    void testTransitions(String testName, int expectedOrdinal, DiskSyncState expectedFromState, DiskSyncState expectedToState,
            DiskSyncEvent expectedInitiatedEvent, DiskSyncEvent expectedFailureEvent, DiskSyncState expectedFailureState) {
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
                { "Trigger moves from init to disk sync init state",                    1, INIT_STATE,                 DISK_SYNC_INIT_STATE,       DISK_SYNC_TRIGGER_EVENT,           FAILURE_EVENT, null },
                { "Process finished moves to finished state",                           2, DISK_SYNC_INIT_STATE, DISK_SYNC_FINISHED_STATE,     DISK_SYNC_PROCESS_FINISHED_EVENT,  FAILURE_EVENT, null },
                { "Finalized moves from finished to final state",                       3, DISK_SYNC_FINISHED_STATE,   FINAL_STATE,                FINALIZED_EVENT,                   null,            null },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    private static final class TransitionKeeper {

        private final int ordinal;

        private final DiskSyncState fromState;

        private final DiskSyncState toState;

        private final DiskSyncEvent initiatedEvent;

        private final DiskSyncEvent failureEvent;

        private final DiskSyncState failureState;

        TransitionKeeper(int ordinal, DiskSyncState fromState, DiskSyncState toState, DiskSyncEvent initiatedEvent,
                DiskSyncEvent failureEvent, DiskSyncState failureState) {
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

        DiskSyncState getFromState() {
            return fromState;
        }

        DiskSyncState getToState() {
            return toState;
        }

        DiskSyncEvent getInitiatedEvent() {
            return initiatedEvent;
        }

        DiskSyncEvent getFailureEvent() {
            return failureEvent;
        }

        DiskSyncState getFailureState() {
            return failureState;
        }
    }
}
