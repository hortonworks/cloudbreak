package com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config;

import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent.SECURITY_GROUP_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent.SECURITY_GROUP_VALIDATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent.SECURITY_GROUP_VALIDATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent.SECURITY_GROUP_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationState.SECURITY_GROUP_VALIDATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationState.SECURITY_GROUP_VALIDATION_RESULT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationState.SECURITY_GROUP_VALIDATION_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition;

class SecurityGroupValidationFlowConfigTest {

    private final SecurityGroupValidationFlowConfig underTest = new SecurityGroupValidationFlowConfig();

    @Test
    void initEventsContainValidationTrigger() {
        List<SecurityGroupValidationEvent> initEvents = Arrays.asList(underTest.getInitEvents());
        assertTrue(initEvents.contains(SECURITY_GROUP_VALIDATION_EVENT));
    }

    @Test
    void validationStateTransitionsToResultOnFinishedEvent() {
        Transition<SecurityGroupValidationState, SecurityGroupValidationEvent> transition = underTest.getTransitions().stream()
                .filter(t -> t.getSource() == SECURITY_GROUP_VALIDATION_STATE
                        && t.getEvent() == SECURITY_GROUP_VALIDATION_FINISHED_EVENT)
                .findFirst()
                .orElseThrow();
        assertEquals(SECURITY_GROUP_VALIDATION_RESULT_STATE, transition.getTarget());
        assertEquals(SECURITY_GROUP_VALIDATION_FAILED_EVENT, transition.getFailureEvent());
    }

    @Test
    void edgeConfigUsesValidationFailedState() {
        assertEquals(INIT_STATE, underTest.getEdgeConfig().getInitState());
        assertEquals(FINAL_STATE, underTest.getEdgeConfig().getFinalState());
        assertEquals(SECURITY_GROUP_VALIDATION_FAILED_STATE, underTest.getEdgeConfig().getDefaultFailureState());
        assertEquals(SECURITY_GROUP_VALIDATION_FAILURE_HANDLED_EVENT, underTest.getEdgeConfig().getFailureHandled());
    }
}
