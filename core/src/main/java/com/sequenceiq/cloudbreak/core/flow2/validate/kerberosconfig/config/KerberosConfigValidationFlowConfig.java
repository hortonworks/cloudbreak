package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config;

import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.BIND_USER_CREATION_STARTED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.FREEIPA_EXISTS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_EXISTS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationState.CHECK_FREEIPA_EXISTS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationState.CREATE_BIND_USER_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationState.POLL_BIND_USER_CREATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationState.VALIDATE_KERBEROS_CONFIG_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationState.VALIDATE_KERBEROS_CONFIG_STATE;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class KerberosConfigValidationFlowConfig extends AbstractFlowConfiguration<KerberosConfigValidationState, KerberosConfigValidationEvent>
        implements RetryableFlowConfiguration<KerberosConfigValidationEvent> {

    private static final List<Transition<KerberosConfigValidationState, KerberosConfigValidationEvent>> TRANSITIONS =
            new Builder<KerberosConfigValidationState, KerberosConfigValidationEvent>()
                    .defaultFailureEvent(VALIDATE_KERBEROS_CONFIG_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(CHECK_FREEIPA_EXISTS_STATE)
                    .event(VALIDATE_KERBEROS_CONFIG_EVENT).defaultFailureEvent()

                    .from(CHECK_FREEIPA_EXISTS_STATE)
                    .to(VALIDATE_KERBEROS_CONFIG_STATE)
                    .event(VALIDATE_KERBEROS_CONFIG_EXISTS_EVENT).defaultFailureEvent()

                    .from(CHECK_FREEIPA_EXISTS_STATE)
                    .to(CREATE_BIND_USER_STATE)
                    .event(FREEIPA_EXISTS_EVENT).defaultFailureEvent()

                    .from(CREATE_BIND_USER_STATE)
                    .to(POLL_BIND_USER_CREATION_STATE)
                    .event(BIND_USER_CREATION_STARTED_EVENT).defaultFailureEvent()

                    .from(POLL_BIND_USER_CREATION_STATE)
                    .to(VALIDATE_KERBEROS_CONFIG_STATE)
                    .event(VALIDATE_KERBEROS_CONFIG_EXISTS_EVENT).defaultFailureEvent()

                    .from(VALIDATE_KERBEROS_CONFIG_STATE)
                    .to(FINAL_STATE)
                    .event(VALIDATE_KERBEROS_CONFIG_FINISHED_EVENT).defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<KerberosConfigValidationState, KerberosConfigValidationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, VALIDATE_KERBEROS_CONFIG_FAILED_STATE, VALIDATE_KERBEROS_CONFIG_FAILURE_HANDLED_EVENT);

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

    public KerberosConfigValidationFlowConfig() {
        super(KerberosConfigValidationState.class, KerberosConfigValidationEvent.class);
    }

    @Override
    protected List<Transition<KerberosConfigValidationState, KerberosConfigValidationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<KerberosConfigValidationState, KerberosConfigValidationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public KerberosConfigValidationEvent[] getEvents() {
        return KerberosConfigValidationEvent.values();
    }

    @Override
    public KerberosConfigValidationEvent[] getInitEvents() {
        return new KerberosConfigValidationEvent[]{
                VALIDATE_KERBEROS_CONFIG_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Validate kerberos config for stack";
    }

    @Override
    public KerberosConfigValidationEvent getRetryableEvent() {
        return VALIDATE_KERBEROS_CONFIG_FAILURE_HANDLED_EVENT;
    }

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}
