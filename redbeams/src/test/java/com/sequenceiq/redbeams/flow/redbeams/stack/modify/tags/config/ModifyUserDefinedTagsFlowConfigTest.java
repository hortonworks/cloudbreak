package com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.config;

import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.ModifyUserDefinedTagsState.FINAL_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.ModifyUserDefinedTagsState.INIT_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.ModifyUserDefinedTagsState.MODIFY_USER_DEFINED_TAGS_CLOUD_RESOURCES_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.ModifyUserDefinedTagsState.MODIFY_USER_DEFINED_TAGS_FAILED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.ModifyUserDefinedTagsState.MODIFY_USER_DEFINED_TAGS_FINISHED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.ModifyUserDefinedTagsState.MODIFY_USER_DEFINED_TAGS_STACK_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FINALIZE_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.MODIFY_USER_DEFINED_TAGS_REDBEAMS_STACK_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.MODIFY_USER_DEFINED_TAGS_REDBEAMS_START_EVENT;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.ModifyUserDefinedTagsState;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors;

class ModifyUserDefinedTagsFlowConfigTest {
    @Test
    @DisplayName("Transitions should form the expected state graph with correct from/to/event mappings")
    void testTransitionsShouldFormTheExpectedStateGraphWithCorrectEventMappings() throws IllegalAccessException {
        List<? extends AbstractFlowConfiguration.Transition<?, ?>> transitions = new ModifyUserDefinedTagsFlowConfig().getTransitions();

        List<FlowTransition> expectedTransitions = List.of(
                new FlowTransition(INIT_STATE,
                        MODIFY_USER_DEFINED_TAGS_CLOUD_RESOURCES_STATE,
                        MODIFY_USER_DEFINED_TAGS_REDBEAMS_START_EVENT),
                new FlowTransition(MODIFY_USER_DEFINED_TAGS_CLOUD_RESOURCES_STATE,
                        MODIFY_USER_DEFINED_TAGS_STACK_STATE,
                        MODIFY_USER_DEFINED_TAGS_REDBEAMS_STACK_EVENT),
                new FlowTransition(MODIFY_USER_DEFINED_TAGS_STACK_STATE,
                        MODIFY_USER_DEFINED_TAGS_FINISHED_STATE,
                        FINISH_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT),
                new FlowTransition(MODIFY_USER_DEFINED_TAGS_FINISHED_STATE,
                        FINAL_STATE,
                        FINALIZE_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT)
        );

        assertEquals(expectedTransitions.size(), transitions.size(),
                "The number of transitions does not match the expected number");

        for (int i = 0; i < transitions.size(); i++) {
            AbstractFlowConfiguration.Transition<?, ?> transition = transitions.get(i);
            FlowTransition expected = expectedTransitions.get(i);

            assertEquals(expected.from(), transition.getSource(),   "Unexpected 'from' state at index " + i);
            assertEquals(expected.to(),   transition.getTarget(),   "Unexpected 'to' state at index " + i);
            assertEquals(expected.event(), extractEvent(transition), "Unexpected event at index " + i);
        }
    }

    @Test
    @DisplayName("Transitions should use default failure event for each step")
    void testTransitionsShouldUseDefaultFailureEventForEachStep() {
        assertDefaultFailureEventForAllTransitions(
                new ModifyUserDefinedTagsFlowConfig().getTransitions()
        );
    }

    @Test
    @DisplayName("Edge config should define specific init, final and default failure states and failure handled event")
    void testEdgeConfigContainsExpectedStatesAndEvent() {
        AbstractFlowConfiguration.FlowEdgeConfig<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors>
                configurationEdge = new ModifyUserDefinedTagsFlowConfig().getEdgeConfig();

        assertEquals(INIT_STATE, configurationEdge.getInitState());
        assertEquals(FINAL_STATE, configurationEdge.getFinalState());
        assertEquals(MODIFY_USER_DEFINED_TAGS_FAILED_STATE, configurationEdge.getDefaultFailureState());
        assertEquals(HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT, configurationEdge.getFailureHandled());
    }

    private static void assertDefaultFailureEventForAllTransitions(List<? extends AbstractFlowConfiguration.Transition<?, ?>> transitions) {
        for (int i = 0; i < transitions.size(); i++) {
            AbstractFlowConfiguration.Transition<?, ?> transition = transitions.get(i);
            assertEquals(FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT, transition.getFailureEvent(),
                    "Unexpected failure event at transition index " + i);
            assertNull(transition.getFailureState(), "Failure state should be null for default failure event at transition index " + i);
        }
    }

    private static FlowEvent extractEvent(AbstractFlowConfiguration.Transition<?, ?> transition) throws IllegalAccessException {
        Field field = ReflectionUtils.findField(AbstractFlowConfiguration.Transition.class, "event");
        ReflectionUtils.makeAccessible(requireNonNull(field));
        return (FlowEvent) field.get(transition);
    }

    private record FlowTransition(FlowState from, FlowState to, FlowEvent event) {
    }
}