package com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config;

import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent.SECURITY_GROUP_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent.SECURITY_GROUP_VALIDATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent.SECURITY_GROUP_VALIDATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent.SECURITY_GROUP_VALIDATION_FAIL_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent.SECURITY_GROUP_VALIDATION_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent.SECURITY_GROUP_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationState.SECURITY_GROUP_VALIDATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationState.SECURITY_GROUP_VALIDATION_RESULT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationState.SECURITY_GROUP_VALIDATION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class SecurityGroupValidationFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<SecurityGroupValidationState, SecurityGroupValidationEvent>
        implements RetryableFlowConfiguration<SecurityGroupValidationEvent> {

    private static final SecurityGroupValidationEvent[] INIT_EVENTS = {SECURITY_GROUP_VALIDATION_EVENT};

    private static final FlowEdgeConfig<SecurityGroupValidationState, SecurityGroupValidationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SECURITY_GROUP_VALIDATION_FAILED_STATE, SECURITY_GROUP_VALIDATION_FAILURE_HANDLED_EVENT);

    private static final List<Transition<SecurityGroupValidationState, SecurityGroupValidationEvent>> TRANSITIONS =
            new Builder<SecurityGroupValidationState, SecurityGroupValidationEvent>()
                    .defaultFailureEvent(SECURITY_GROUP_VALIDATION_FAIL_EVENT)

                    .from(INIT_STATE)
                    .to(SECURITY_GROUP_VALIDATION_STATE)
                    .event(SECURITY_GROUP_VALIDATION_EVENT)
                    .defaultFailureEvent()

                    .from(SECURITY_GROUP_VALIDATION_STATE)
                    .to(SECURITY_GROUP_VALIDATION_RESULT_STATE)
                    .event(SECURITY_GROUP_VALIDATION_FINISHED_EVENT)
                    .failureEvent(SECURITY_GROUP_VALIDATION_FAILED_EVENT)

                    .from(SECURITY_GROUP_VALIDATION_RESULT_STATE)
                    .to(FINAL_STATE)
                    .event(SECURITY_GROUP_VALIDATION_FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    public SecurityGroupValidationFlowConfig() {
        super(SecurityGroupValidationState.class, SecurityGroupValidationEvent.class);
    }

    @Override
    protected List<Transition<SecurityGroupValidationState, SecurityGroupValidationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<SecurityGroupValidationState, SecurityGroupValidationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SecurityGroupValidationEvent[] getEvents() {
        return SecurityGroupValidationEvent.values();
    }

    @Override
    public SecurityGroupValidationEvent[] getInitEvents() {
        return INIT_EVENTS;
    }

    @Override
    public String getDisplayName() {
        return "Validate security groups";
    }

    @Override
    public SecurityGroupValidationEvent getRetryableEvent() {
        return SECURITY_GROUP_VALIDATION_FAILURE_HANDLED_EVENT;
    }
}
