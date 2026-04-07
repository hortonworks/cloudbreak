package com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.ModifyUserDefinedTagsState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.ModifyUserDefinedTagsState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.ModifyUserDefinedTagsState.MODIFY_USER_DEFINED_TAGS_CLOUD_RESOURCES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.ModifyUserDefinedTagsState.MODIFY_USER_DEFINED_TAGS_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.ModifyUserDefinedTagsState.MODIFY_USER_DEFINED_TAGS_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.ModifyUserDefinedTagsState.MODIFY_USER_DEFINED_TAGS_STACK_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsStateSelectors.FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsStateSelectors.FINALIZE_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsStateSelectors.HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsStateSelectors.MODIFY_USER_DEFINED_TAGS_STACK_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsStateSelectors.MODIFY_USER_DEFINED_TAGS_START_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.ModifyUserDefinedTagsState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class ModifyUserDefinedTagsFlowConfig extends StackStatusFinalizerAbstractFlowConfig<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors> {

    private static final List<AbstractFlowConfiguration.Transition<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors>> TRANSITIONS =
            new AbstractFlowConfiguration.Transition.Builder<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors>()
                    .defaultFailureEvent(FAILED_MODIFY_USER_DEFINED_TAGS_EVENT)

                    .from(INIT_STATE)
                    .to(MODIFY_USER_DEFINED_TAGS_CLOUD_RESOURCES_STATE)
                    .event(MODIFY_USER_DEFINED_TAGS_START_EVENT)
                    .defaultFailureEvent()

                    .from(MODIFY_USER_DEFINED_TAGS_CLOUD_RESOURCES_STATE)
                    .to(MODIFY_USER_DEFINED_TAGS_STACK_STATE)
                    .event(MODIFY_USER_DEFINED_TAGS_STACK_EVENT)
                    .defaultFailureEvent()

                    .from(MODIFY_USER_DEFINED_TAGS_STACK_STATE)
                    .to(MODIFY_USER_DEFINED_TAGS_FINISHED_STATE)
                    .event(FINISH_MODIFY_USER_DEFINED_TAGS_EVENT)
                    .defaultFailureEvent()

                    .from(MODIFY_USER_DEFINED_TAGS_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(FINALIZE_MODIFY_USER_DEFINED_TAGS_EVENT)
                    .defaultFailureEvent()

                    .build();

    protected ModifyUserDefinedTagsFlowConfig() {
        super(ModifyUserDefinedTagsState.class, ModifyUserDefinedTagsStateSelectors.class);
    }

    @Override
    protected List<AbstractFlowConfiguration.Transition<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public AbstractFlowConfiguration.FlowEdgeConfig<ModifyUserDefinedTagsState, ModifyUserDefinedTagsStateSelectors> getEdgeConfig() {
        return new AbstractFlowConfiguration.FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, MODIFY_USER_DEFINED_TAGS_FAILED_STATE,
                HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_EVENT);
    }

    @Override
    public ModifyUserDefinedTagsStateSelectors[] getEvents() {
        return ModifyUserDefinedTagsStateSelectors.values();
    }

    @Override
    public ModifyUserDefinedTagsStateSelectors[] getInitEvents() {
        return new ModifyUserDefinedTagsStateSelectors[] {MODIFY_USER_DEFINED_TAGS_START_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Modify user defined tags on stack";
    }
}
