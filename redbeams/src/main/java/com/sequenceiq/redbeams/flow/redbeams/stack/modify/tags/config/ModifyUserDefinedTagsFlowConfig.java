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

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.ModifyUserDefinedTagsState;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors;

@Component
public class ModifyUserDefinedTagsFlowConfig extends AbstractFlowConfiguration<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors>
        implements RetryableFlowConfiguration<ModifyUserDefinedTagsStateSelectors> {

    private static final List<Transition<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors>> TRANSITIONS =
            new Transition.Builder<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors>()
                    .defaultFailureEvent(FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT)

                    .from(INIT_STATE)
                    .to(MODIFY_USER_DEFINED_TAGS_CLOUD_RESOURCES_STATE)
                    .event(MODIFY_USER_DEFINED_TAGS_REDBEAMS_START_EVENT)
                    .defaultFailureEvent()

                    .from(MODIFY_USER_DEFINED_TAGS_CLOUD_RESOURCES_STATE)
                    .to(MODIFY_USER_DEFINED_TAGS_STACK_STATE)
                    .event(MODIFY_USER_DEFINED_TAGS_REDBEAMS_STACK_EVENT)
                    .defaultFailureEvent()

                    .from(MODIFY_USER_DEFINED_TAGS_STACK_STATE)
                    .to(MODIFY_USER_DEFINED_TAGS_FINISHED_STATE)
                    .event(FINISH_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT)
                    .defaultFailureEvent()

                    .from(MODIFY_USER_DEFINED_TAGS_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(FINALIZE_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT)
                    .defaultFailureEvent()

                    .build();

    protected ModifyUserDefinedTagsFlowConfig() {
        super(ModifyUserDefinedTagsState.class, ModifyUserDefinedTagsStateSelectors.class);
    }

    @Override
    protected List<Transition<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, MODIFY_USER_DEFINED_TAGS_FAILED_STATE, HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT);
    }

    @Override
    public ModifyUserDefinedTagsStateSelectors[] getEvents() {
        return ModifyUserDefinedTagsStateSelectors.values();
    }

    @Override
    public ModifyUserDefinedTagsStateSelectors[] getInitEvents() {
        return new ModifyUserDefinedTagsStateSelectors[] {MODIFY_USER_DEFINED_TAGS_REDBEAMS_START_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Modify user defined tags on external database";
    }

    @Override
    public ModifyUserDefinedTagsStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT;
    }
}
