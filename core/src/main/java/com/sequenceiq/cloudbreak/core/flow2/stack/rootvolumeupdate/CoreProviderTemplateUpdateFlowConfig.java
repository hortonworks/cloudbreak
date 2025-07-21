package com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate;

import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateState.CORE_PROVIDER_TEMPLATE_UPDATE_FAIL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateState.CORE_PROVIDER_TEMPLATE_UPDATE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateState.CORE_PROVIDER_TEMPLATE_UPDATE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateState.CORE_PROVIDER_TEMPLATE_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;

@Component
public class CoreProviderTemplateUpdateFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<CoreProviderTemplateUpdateState, CoreProviderTemplateUpdateFlowEvent> {

    private static final List<Transition<CoreProviderTemplateUpdateState, CoreProviderTemplateUpdateFlowEvent>> TRANSITIONS =
            new Transition.Builder<CoreProviderTemplateUpdateState, CoreProviderTemplateUpdateFlowEvent>()
                    .defaultFailureEvent(CORE_PROVIDER_TEMPLATE_UPDATE_FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(CORE_PROVIDER_TEMPLATE_VALIDATION_STATE)
                    .event(CORE_PROVIDER_TEMPLATE_UPDATE_TRIGGER_EVENT)
                    .defaultFailureEvent()

                    .from(CORE_PROVIDER_TEMPLATE_VALIDATION_STATE)
                    .to(CORE_PROVIDER_TEMPLATE_UPDATE_STATE)
                    .event(CORE_PROVIDER_TEMPLATE_VALIDATION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(CORE_PROVIDER_TEMPLATE_UPDATE_STATE)
                    .to(CORE_PROVIDER_TEMPLATE_UPDATE_FINISHED_STATE)
                    .event(CORE_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(CORE_PROVIDER_TEMPLATE_UPDATE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(CORE_PROVIDER_TEMPLATE_UPDATE_FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<CoreProviderTemplateUpdateState, CoreProviderTemplateUpdateFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(
                    INIT_STATE,
                    FINAL_STATE,
                    CORE_PROVIDER_TEMPLATE_UPDATE_FAIL_STATE,
                    CORE_PROVIDER_TEMPLATE_UPDATE_FAIL_HANDLED_EVENT);

    public CoreProviderTemplateUpdateFlowConfig() {
        super(CoreProviderTemplateUpdateState.class, CoreProviderTemplateUpdateFlowEvent.class);
    }

    @Override
    protected List<Transition<CoreProviderTemplateUpdateState, CoreProviderTemplateUpdateFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<CoreProviderTemplateUpdateState, CoreProviderTemplateUpdateFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public CoreProviderTemplateUpdateFlowEvent[] getEvents() {
        return CoreProviderTemplateUpdateFlowEvent.values();
    }

    @Override
    public CoreProviderTemplateUpdateFlowEvent[] getInitEvents() {
        return new CoreProviderTemplateUpdateFlowEvent[] {CORE_PROVIDER_TEMPLATE_UPDATE_TRIGGER_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Update AWS Launch Template";
    }
}
