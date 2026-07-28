package com.sequenceiq.freeipa.flow.freeipa.rollingvscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizer;

@ExtendWith(MockitoExtension.class)
class FreeIpaRollingVerticalScaleFlowConfigTest {

    @Mock
    private StackStatusFinalizer stackStatusFinalizer;

    @InjectMocks
    private FreeIpaRollingVerticalScaleFlowConfig underTest;

    @Test
    void testGetTransitionsContainsAllExpectedStates() {
        List<AbstractFlowConfiguration.Transition<FreeIpaRollingVerticalScaleState, FreeIpaRollingVerticalScaleEvent>> transitions
                = underTest.getTransitions();

        assertEquals(10, transitions.size(), "Expected 10 transitions in the flow config");

        Set<FreeIpaRollingVerticalScaleState> sourceStates = transitions.stream()
                .map(AbstractFlowConfiguration.Transition::getSource)
                .collect(Collectors.toSet());
        assertTrue(sourceStates.contains(FreeIpaRollingVerticalScaleState.INIT_STATE));
        assertTrue(sourceStates.contains(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_DESCRIBE_STATE));
        assertTrue(sourceStates.contains(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_STOP_HEALTH_AGENT_STATE));
        assertTrue(sourceStates.contains(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_STOP_SERVICES_STATE));
        assertTrue(sourceStates.contains(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_STOP_VM_STATE));
        assertTrue(sourceStates.contains(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_RESIZE_STATE));
        assertTrue(sourceStates.contains(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_START_VM_STATE));
        assertTrue(sourceStates.contains(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_HEALTH_CHECK_STATE));
        assertTrue(sourceStates.contains(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_FINISHED_STATE));
    }

    @Test
    void testGetEdgeConfig() {
        AbstractFlowConfiguration.FlowEdgeConfig<FreeIpaRollingVerticalScaleState, FreeIpaRollingVerticalScaleEvent> edgeConfig
                = underTest.getEdgeConfig();

        assertEquals(FreeIpaRollingVerticalScaleState.INIT_STATE, edgeConfig.getInitState());
        assertEquals(FreeIpaRollingVerticalScaleState.FINAL_STATE, edgeConfig.getFinalState());
        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_FAILED_STATE, edgeConfig.getDefaultFailureState());
        assertEquals(FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_FAIL_HANDLED_EVENT, edgeConfig.getFailureHandled());
    }

    @Test
    void testGetInitEvents() {
        FreeIpaRollingVerticalScaleEvent[] initEvents = underTest.getInitEvents();

        assertEquals(1, initEvents.length);
        assertEquals(FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_TRIGGER_EVENT, initEvents[0]);
    }

    @Test
    void testGetEventsContainsAllEnumValues() {
        FreeIpaRollingVerticalScaleEvent[] events = underTest.getEvents();

        Set<FreeIpaRollingVerticalScaleEvent> eventSet = Set.of(events);
        for (FreeIpaRollingVerticalScaleEvent expected : FreeIpaRollingVerticalScaleEvent.values()) {
            assertTrue(eventSet.contains(expected), "Missing event: " + expected);
        }
    }

    @Test
    void testGetDisplayName() {
        assertEquals("Rolling vertical scale on FreeIPA", underTest.getDisplayName());
    }

    @Test
    void testTransitionsHaveCorrectFailureEvents() {
        List<AbstractFlowConfiguration.Transition<FreeIpaRollingVerticalScaleState, FreeIpaRollingVerticalScaleEvent>> transitions
                = underTest.getTransitions();

        for (AbstractFlowConfiguration.Transition<FreeIpaRollingVerticalScaleState, FreeIpaRollingVerticalScaleEvent> transition : transitions) {
            if (FreeIpaRollingVerticalScaleState.INIT_STATE.equals(transition.getSource())) {
                continue;
            }
            if (FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_FINISHED_STATE.equals(transition.getSource())) {
                assertEquals(FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_FAILURE_EVENT, transition.getFailureEvent());
            } else {
                assertNotNull(transition.getFailureEvent(),
                        "Failure event must be set for transition from " + transition.getSource());
            }
        }
    }

    @Test
    void testTransitionOrderMatchesStateMachine() {
        List<AbstractFlowConfiguration.Transition<FreeIpaRollingVerticalScaleState, FreeIpaRollingVerticalScaleEvent>> transitions
                = underTest.getTransitions();

        assertEquals(FreeIpaRollingVerticalScaleState.INIT_STATE, transitions.get(0).getSource());
        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_DESCRIBE_STATE, transitions.get(0).getTarget());

        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_DESCRIBE_STATE, transitions.get(1).getSource());
        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_STOP_HEALTH_AGENT_STATE, transitions.get(1).getTarget());

        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_DESCRIBE_STATE, transitions.get(2).getSource());
        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_FINISHED_STATE, transitions.get(2).getTarget());

        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_STOP_HEALTH_AGENT_STATE, transitions.get(3).getSource());
        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_STOP_SERVICES_STATE, transitions.get(3).getTarget());

        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_STOP_SERVICES_STATE, transitions.get(4).getSource());
        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_STOP_VM_STATE, transitions.get(4).getTarget());

        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_STOP_VM_STATE, transitions.get(5).getSource());
        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_RESIZE_STATE, transitions.get(5).getTarget());

        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_RESIZE_STATE, transitions.get(6).getSource());
        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_START_VM_STATE, transitions.get(6).getTarget());

        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_START_VM_STATE, transitions.get(7).getSource());
        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_HEALTH_CHECK_STATE, transitions.get(7).getTarget());

        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_HEALTH_CHECK_STATE, transitions.get(8).getSource());
        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_FINISHED_STATE, transitions.get(8).getTarget());

        assertEquals(FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_FINISHED_STATE, transitions.get(9).getSource());
        assertEquals(FreeIpaRollingVerticalScaleState.FINAL_STATE, transitions.get(9).getTarget());
    }
}
