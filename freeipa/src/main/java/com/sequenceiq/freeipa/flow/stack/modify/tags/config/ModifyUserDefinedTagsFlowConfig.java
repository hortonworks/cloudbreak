package com.sequenceiq.freeipa.flow.stack.modify.tags.config;

import static com.sequenceiq.freeipa.flow.stack.modify.tags.ModifyUserDefinedTagsState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.ModifyUserDefinedTagsState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.ModifyUserDefinedTagsState.MODIFY_USER_DEFINED_TAGS_CLOUD_RESOURCES_STATE;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.ModifyUserDefinedTagsState.MODIFY_USER_DEFINED_TAGS_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.ModifyUserDefinedTagsState.MODIFY_USER_DEFINED_TAGS_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.ModifyUserDefinedTagsState.MODIFY_USER_DEFINED_TAGS_STACK_STATE;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FAILED_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FINALIZE_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.MODIFY_USER_DEFINED_TAGS_FREEIPA_STACK_EVENT;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.MODIFY_USER_DEFINED_TAGS_FREEIPA_START_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.freeipa.flow.stack.modify.tags.ModifyUserDefinedTagsState;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors;

@Component
public class ModifyUserDefinedTagsFlowConfig extends StackStatusFinalizerAbstractFlowConfig<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors>
        implements RetryableFlowConfiguration<ModifyUserDefinedTagsStateSelectors>, FreeIpaUseCaseAware {

    private static final List<Transition<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors>> TRANSITIONS =
            new Transition.Builder<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors>()
                    .defaultFailureEvent(FAILED_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT)

                    .from(INIT_STATE)
                    .to(MODIFY_USER_DEFINED_TAGS_CLOUD_RESOURCES_STATE)
                    .event(MODIFY_USER_DEFINED_TAGS_FREEIPA_START_EVENT)
                    .defaultFailureEvent()

                    .from(MODIFY_USER_DEFINED_TAGS_CLOUD_RESOURCES_STATE)
                    .to(MODIFY_USER_DEFINED_TAGS_STACK_STATE)
                    .event(MODIFY_USER_DEFINED_TAGS_FREEIPA_STACK_EVENT)
                    .defaultFailureEvent()

                    .from(MODIFY_USER_DEFINED_TAGS_STACK_STATE)
                    .to(MODIFY_USER_DEFINED_TAGS_FINISHED_STATE)
                    .event(FINISH_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT)
                    .defaultFailureEvent()

                    .from(MODIFY_USER_DEFINED_TAGS_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(FINALIZE_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT)
                    .defaultFailureEvent()

                    .build();

    protected ModifyUserDefinedTagsFlowConfig() {
        super(ModifyUserDefinedTagsState.class, ModifyUserDefinedTagsStateSelectors.class);
    }

    @Override
    public UsageProto.CDPFreeIPAStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        return null;
    }

    @Override
    protected List<Transition<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, MODIFY_USER_DEFINED_TAGS_FAILED_STATE, HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT);
    }

    @Override
    public ModifyUserDefinedTagsStateSelectors[] getEvents() {
        return ModifyUserDefinedTagsStateSelectors.values();
    }

    @Override
    public ModifyUserDefinedTagsStateSelectors[] getInitEvents() {
        return new ModifyUserDefinedTagsStateSelectors[] {MODIFY_USER_DEFINED_TAGS_FREEIPA_START_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Modify user defined tags on FreeIPA";
    }

    @Override
    public ModifyUserDefinedTagsStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;
    }
}
