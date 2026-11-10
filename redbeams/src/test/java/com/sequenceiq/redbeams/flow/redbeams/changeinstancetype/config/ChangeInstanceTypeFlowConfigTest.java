package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.config;

import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState.CHANGE_INSTANCE_TYPE_FAILED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState.CHANGE_INSTANCE_TYPE_FINISHED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState.CHANGE_INSTANCE_TYPE_MODIFY_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState.CHANGE_INSTANCE_TYPE_SYNC_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState.CHANGE_INSTANCE_TYPE_VALIDATION_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState.CHANGE_INSTANCE_TYPE_WAIT_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState.FINAL_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState.INIT_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_ALREADY_APPLIED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_MODIFY_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_START_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_SYNC_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_VALIDATION_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_WAIT_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.FAILED_CHANGE_INSTANCE_TYPE_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.FINALIZE_CHANGE_INSTANCE_TYPE_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.HANDLED_FAILED_CHANGE_INSTANCE_TYPE_EVENT;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors;

class ChangeInstanceTypeFlowConfigTest {

    private final ChangeInstanceTypeFlowConfig underTest = new ChangeInstanceTypeFlowConfig();

    @Test
    void testTransitions() throws IllegalAccessException {
        List<FlowTransition> expectedTransitions = List.of(
                new FlowTransition(INIT_STATE, CHANGE_INSTANCE_TYPE_SYNC_STATE, CHANGE_INSTANCE_TYPE_START_EVENT),
                new FlowTransition(CHANGE_INSTANCE_TYPE_SYNC_STATE, CHANGE_INSTANCE_TYPE_VALIDATION_STATE, CHANGE_INSTANCE_TYPE_SYNC_FINISHED_EVENT),
                new FlowTransition(CHANGE_INSTANCE_TYPE_VALIDATION_STATE, CHANGE_INSTANCE_TYPE_MODIFY_STATE, CHANGE_INSTANCE_TYPE_VALIDATION_FINISHED_EVENT),
                new FlowTransition(CHANGE_INSTANCE_TYPE_VALIDATION_STATE, CHANGE_INSTANCE_TYPE_FINISHED_STATE, CHANGE_INSTANCE_TYPE_ALREADY_APPLIED_EVENT),
                new FlowTransition(CHANGE_INSTANCE_TYPE_MODIFY_STATE, CHANGE_INSTANCE_TYPE_WAIT_STATE, CHANGE_INSTANCE_TYPE_MODIFY_FINISHED_EVENT),
                new FlowTransition(CHANGE_INSTANCE_TYPE_WAIT_STATE, CHANGE_INSTANCE_TYPE_FINISHED_STATE, CHANGE_INSTANCE_TYPE_WAIT_FINISHED_EVENT),
                new FlowTransition(CHANGE_INSTANCE_TYPE_FINISHED_STATE, FINAL_STATE, FINALIZE_CHANGE_INSTANCE_TYPE_EVENT),
                new FlowTransition(CHANGE_INSTANCE_TYPE_VALIDATION_STATE, CHANGE_INSTANCE_TYPE_FAILED_STATE, CHANGE_INSTANCE_TYPE_VALIDATION_FAILED_EVENT),
                new FlowTransition(CHANGE_INSTANCE_TYPE_FAILED_STATE, FINAL_STATE, HANDLED_FAILED_CHANGE_INSTANCE_TYPE_EVENT)
        );
        List<? extends AbstractFlowConfiguration.Transition<?, ?>> transitions = underTest.getTransitions();

        assertEquals(expectedTransitions.size(), transitions.size());
        for (int i = 0; i < transitions.size(); i++) {
            AbstractFlowConfiguration.Transition<?, ?> transition = transitions.get(i);
            FlowTransition expected = expectedTransitions.get(i);
            assertEquals(expected.from(), transition.getSource(), "Unexpected 'from' state at index " + i);
            assertEquals(expected.to(), transition.getTarget(), "Unexpected 'to' state at index " + i);
            assertEquals(expected.event(), extractEvent(transition), "Unexpected event at index " + i);
            assertNull(transition.getFailureState(), "Failure state should be null at index " + i);
        }
    }

    @Test
    void testStepsFailWithTheDefaultFailureEvent() {
        List<? extends AbstractFlowConfiguration.Transition<?, ?>> transitions = underTest.getTransitions();

        for (int i = 0; i < transitions.size() - 1; i++) {
            assertEquals(FAILED_CHANGE_INSTANCE_TYPE_EVENT, transitions.get(i).getFailureEvent(), "Unexpected failure event at index " + i);
        }
        assertNull(transitions.getLast().getFailureEvent());
    }

    @Test
    void testEdgeConfig() {
        AbstractFlowConfiguration.FlowEdgeConfig<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors> edgeConfig = underTest.getEdgeConfig();

        assertEquals(INIT_STATE, edgeConfig.getInitState());
        assertEquals(FINAL_STATE, edgeConfig.getFinalState());
        assertEquals(CHANGE_INSTANCE_TYPE_FAILED_STATE, edgeConfig.getDefaultFailureState());
        assertEquals(HANDLED_FAILED_CHANGE_INSTANCE_TYPE_EVENT, edgeConfig.getFailureHandled());
    }

    @Test
    void testInitAndRetryableEvents() {
        assertArrayEquals(new ChangeInstanceTypeStateSelectors[]{CHANGE_INSTANCE_TYPE_START_EVENT}, underTest.getInitEvents());
        assertEquals(HANDLED_FAILED_CHANGE_INSTANCE_TYPE_EVENT, underTest.getRetryableEvent());
        assertArrayEquals(ChangeInstanceTypeStateSelectors.values(), underTest.getEvents());
    }

    private static FlowEvent extractEvent(AbstractFlowConfiguration.Transition<?, ?> transition) throws IllegalAccessException {
        Field field = ReflectionUtils.findField(AbstractFlowConfiguration.Transition.class, "event");
        ReflectionUtils.makeAccessible(requireNonNull(field));
        return (FlowEvent) field.get(transition);
    }

    private record FlowTransition(FlowState from, FlowState to, FlowEvent event) {
    }
}
