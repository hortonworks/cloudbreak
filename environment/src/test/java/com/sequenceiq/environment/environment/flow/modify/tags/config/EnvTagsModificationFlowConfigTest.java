package com.sequenceiq.environment.environment.flow.modify.tags.config;

import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.ENVIRONMENT_TAGS_MODIFICATION_START_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_DATAHUBS_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_DATALAKE_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_FREEIPA_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_REDBEAMS_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FINALIZE_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_ENVIRONMENT_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_USER_DEFINED_TAGS_DATAHUBS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_USER_DEFINED_TAGS_DATALAKE_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

class EnvTagsModificationFlowConfigTest {
    @Test
    @DisplayName("Transitions should form the expected state graph with correct from/to/event mappings")
    void testTransitionsShouldFormTheExpectedStateGraphWithCorrectEventMappings() throws IllegalAccessException {
        List<? extends AbstractFlowConfiguration.Transition<?, ?>> transitions = new EnvTagsModificationFlowConfig().getTransitions();

        List<FlowTransition> expectedTransitions = List.of(
                new FlowTransition(INIT_STATE,
                        ENVIRONMENT_TAGS_MODIFICATION_START_STATE,
                        START_MODIFY_ENVIRONMENT_TAGS_EVENT),
                new FlowTransition(ENVIRONMENT_TAGS_MODIFICATION_START_STATE,
                        USER_DEFINED_TAGS_MODIFICATION_FREEIPA_STATE,
                        START_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT),
                new FlowTransition(USER_DEFINED_TAGS_MODIFICATION_FREEIPA_STATE,
                        USER_DEFINED_TAGS_MODIFICATION_DATALAKE_STATE,
                        START_MODIFY_USER_DEFINED_TAGS_DATALAKE_EVENT),
                new FlowTransition(USER_DEFINED_TAGS_MODIFICATION_DATALAKE_STATE,
                        USER_DEFINED_TAGS_MODIFICATION_DATAHUBS_STATE,
                        START_MODIFY_USER_DEFINED_TAGS_DATAHUBS_EVENT),
                new FlowTransition(USER_DEFINED_TAGS_MODIFICATION_DATAHUBS_STATE,
                        USER_DEFINED_TAGS_MODIFICATION_REDBEAMS_STATE,
                        START_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT),
                new FlowTransition(USER_DEFINED_TAGS_MODIFICATION_REDBEAMS_STATE,
                        USER_DEFINED_TAGS_MODIFICATION_FINISHED_STATE,
                        FINISH_MODIFY_USER_DEFINED_TAGS_EVENT),
                new FlowTransition(USER_DEFINED_TAGS_MODIFICATION_FINISHED_STATE,
                        FINAL_STATE,
                        FINALIZE_MODIFY_USER_DEFINED_TAGS_EVENT)
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
                new EnvTagsModificationFlowConfig().getTransitions()
        );
    }

    @Test
    @DisplayName("Edge config should define specific init, final and default failure states and failure handled event")
    void testEdgeConfigContainsExpectedStatesAndEvent() {
        AbstractFlowConfiguration.FlowEdgeConfig<EnvTagsModificationState, EnvTagsModificationStateSelectors>
                configurationEdge = new EnvTagsModificationFlowConfig().getEdgeConfig();

        assertEquals(INIT_STATE, configurationEdge.getInitState());
        assertEquals(FINAL_STATE, configurationEdge.getFinalState());
        assertEquals(USER_DEFINED_TAGS_MODIFICATION_FAILED_STATE, configurationEdge.getDefaultFailureState());
        assertEquals(HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_EVENT, configurationEdge.getFailureHandled());
    }

    private static void assertDefaultFailureEventForAllTransitions(List<? extends AbstractFlowConfiguration.Transition<?, ?>> transitions) {
        for (int i = 0; i < transitions.size(); i++) {
            AbstractFlowConfiguration.Transition<?, ?> transition = transitions.get(i);
            assertEquals(FAILED_MODIFY_USER_DEFINED_TAGS_EVENT, transition.getFailureEvent(),
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