package com.sequenceiq.environment.environment.flow.modify.tags.config;

import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.ENVIRONMENT_TAGS_MODIFICATION_START_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_DATAHUBS_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_DATALAKE_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_FREEIPA_STATE;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FINALIZE_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_ENVIRONMENT_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_USER_DEFINED_TAGS_DATAHUBS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_USER_DEFINED_TAGS_DATALAKE_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationState;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class EnvTagsModificationFlowConfig extends
        AbstractFlowConfiguration<EnvTagsModificationState, EnvTagsModificationStateSelectors>
        implements RetryableFlowConfiguration<EnvTagsModificationStateSelectors>, EnvironmentUseCaseAware {

    private static final List<Transition<EnvTagsModificationState, EnvTagsModificationStateSelectors>> TRANSITIONS =
            new Transition.Builder<EnvTagsModificationState, EnvTagsModificationStateSelectors>()
                    .defaultFailureEvent(FAILED_MODIFY_USER_DEFINED_TAGS_EVENT)

                    .from(INIT_STATE).to(ENVIRONMENT_TAGS_MODIFICATION_START_STATE)
                    .event(START_MODIFY_ENVIRONMENT_TAGS_EVENT).defaultFailureEvent()

                    .from(ENVIRONMENT_TAGS_MODIFICATION_START_STATE).to(USER_DEFINED_TAGS_MODIFICATION_FREEIPA_STATE)
                    .event(START_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT).defaultFailureEvent()

                    .from(USER_DEFINED_TAGS_MODIFICATION_FREEIPA_STATE).to(USER_DEFINED_TAGS_MODIFICATION_DATALAKE_STATE)
                    .event(START_MODIFY_USER_DEFINED_TAGS_DATALAKE_EVENT).defaultFailureEvent()

                    .from(USER_DEFINED_TAGS_MODIFICATION_DATALAKE_STATE).to(USER_DEFINED_TAGS_MODIFICATION_DATAHUBS_STATE)
                    .event(START_MODIFY_USER_DEFINED_TAGS_DATAHUBS_EVENT).defaultFailureEvent()

                    .from(USER_DEFINED_TAGS_MODIFICATION_DATAHUBS_STATE).to(USER_DEFINED_TAGS_MODIFICATION_FINISHED_STATE)
                    .event(FINISH_MODIFY_USER_DEFINED_TAGS_EVENT).defaultFailureEvent()

                    .from(USER_DEFINED_TAGS_MODIFICATION_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZE_MODIFY_USER_DEFINED_TAGS_EVENT).defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<EnvTagsModificationState, EnvTagsModificationStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, USER_DEFINED_TAGS_MODIFICATION_FAILED_STATE, HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_EVENT);

    protected EnvTagsModificationFlowConfig() {
        super(EnvTagsModificationState.class, EnvTagsModificationStateSelectors.class);
    }

    @Override
    protected List<Transition<EnvTagsModificationState, EnvTagsModificationStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<EnvTagsModificationState, EnvTagsModificationStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public EnvTagsModificationStateSelectors[] getEvents() {
        return EnvTagsModificationStateSelectors.values();
    }

    @Override
    public EnvTagsModificationStateSelectors[] getInitEvents() {
        return new EnvTagsModificationStateSelectors[]{START_MODIFY_ENVIRONMENT_TAGS_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Modify environment tags";
    }

    @Override
    public EnvTagsModificationStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;
    }

    @Override
    public UsageProto.CDPEnvironmentStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        return null;
    }
}