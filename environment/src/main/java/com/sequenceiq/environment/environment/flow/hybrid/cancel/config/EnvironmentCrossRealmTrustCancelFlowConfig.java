package com.sequenceiq.environment.environment.flow.hybrid.cancel.config;


import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_CONFIG_REMOVAL_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_SALT_UPDATE_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_TRUST_ENTITY_DELETE_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_VALIDATION_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.FAILED_TRUST_CANCEL_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.FINALIZE_TRUST_CANCEL_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.FINISH_TRUST_CANCEL_CONFIG_REMOVAL_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.HANDLED_FAILED_TRUST_CANCEL_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.TRUST_CANCEL_CONFIG_REMOVAL_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.TRUST_CANCEL_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.TRUST_CANCEL_SALT_UPDATE_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.TRUST_CANCEL_TRUST_ENTITY_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.TRUST_CANCEL_VALIDATION_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class EnvironmentCrossRealmTrustCancelFlowConfig extends AbstractFlowConfiguration<EnvironmentCrossRealmTrustCancelState,
        EnvironmentCrossRealmTrustCancelStateSelectors>
        implements RetryableFlowConfiguration<EnvironmentCrossRealmTrustCancelStateSelectors>, EnvironmentUseCaseAware {

    private static final List<Transition<EnvironmentCrossRealmTrustCancelState, EnvironmentCrossRealmTrustCancelStateSelectors>> TRANSITIONS =
            new Transition.Builder<EnvironmentCrossRealmTrustCancelState, EnvironmentCrossRealmTrustCancelStateSelectors>()
            .defaultFailureEvent(FAILED_TRUST_CANCEL_EVENT)

            .from(INIT_STATE)
            .to(TRUST_CANCEL_VALIDATION_STATE)
            .event(TRUST_CANCEL_VALIDATION_EVENT)
            .defaultFailureEvent()

            .from(TRUST_CANCEL_VALIDATION_STATE)
            .to(TRUST_CANCEL_STATE)
            .event(TRUST_CANCEL_EVENT)
            .defaultFailureEvent()

            // FreeIPA cross-realm trust cancel runs before CM config removal so that the realm name
            // is still present in FreeIPA when we need to remove it from CM clusters.
            // The trust entity itself is only deleted AFTER config removal succeeds (see TRUST_CANCEL_TRUST_ENTITY_DELETE_STATE).
            .from(TRUST_CANCEL_STATE)
            .to(TRUST_CANCEL_CONFIG_REMOVAL_STATE)
            .event(TRUST_CANCEL_CONFIG_REMOVAL_EVENT)
            .defaultFailureEvent()

            .from(TRUST_CANCEL_CONFIG_REMOVAL_STATE)
            .to(TRUST_CANCEL_TRUST_ENTITY_DELETE_STATE)
            .event(TRUST_CANCEL_TRUST_ENTITY_DELETE_EVENT)
            .defaultFailureEvent()

            .from(TRUST_CANCEL_TRUST_ENTITY_DELETE_STATE)
            .to(TRUST_CANCEL_SALT_UPDATE_STATE)
            .event(TRUST_CANCEL_SALT_UPDATE_EVENT)
            .defaultFailureEvent()

            // Salt update after trust entity deletion removes /etc/krb5.conf.d/trust.conf from clusters.
            // Now that the trust entity is gone KerberosPillarConfigGenerator returns an empty trust pillar,
            // so the file.absent Salt state takes effect.
            .from(TRUST_CANCEL_SALT_UPDATE_STATE)
            .to(TRUST_CANCEL_FINISHED_STATE)
            .event(FINISH_TRUST_CANCEL_CONFIG_REMOVAL_EVENT)
            .defaultFailureEvent()

            .from(TRUST_CANCEL_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(FINALIZE_TRUST_CANCEL_EVENT)
            .defaultFailureEvent()

            .build();

    protected EnvironmentCrossRealmTrustCancelFlowConfig() {
        super(EnvironmentCrossRealmTrustCancelState.class, EnvironmentCrossRealmTrustCancelStateSelectors.class);
    }

    @Override
    protected List<Transition<EnvironmentCrossRealmTrustCancelState, EnvironmentCrossRealmTrustCancelStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<EnvironmentCrossRealmTrustCancelState, EnvironmentCrossRealmTrustCancelStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(
                INIT_STATE,
                FINAL_STATE,
                TRUST_CANCEL_FAILED_STATE,
                HANDLED_FAILED_TRUST_CANCEL_EVENT
        );
    }

    @Override
    public EnvironmentCrossRealmTrustCancelStateSelectors[] getEvents() {
        return EnvironmentCrossRealmTrustCancelStateSelectors.values();
    }

    @Override
    public EnvironmentCrossRealmTrustCancelStateSelectors[] getInitEvents() {
        return new EnvironmentCrossRealmTrustCancelStateSelectors[] {TRUST_CANCEL_VALIDATION_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Cross Realm Trust cancel";
    }

    @Override
    public EnvironmentCrossRealmTrustCancelStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_TRUST_CANCEL_EVENT;
    }

    @Override
    public UsageProto.CDPEnvironmentStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        return UsageProto.CDPEnvironmentStatus.Value.UNSET;
    }
}
