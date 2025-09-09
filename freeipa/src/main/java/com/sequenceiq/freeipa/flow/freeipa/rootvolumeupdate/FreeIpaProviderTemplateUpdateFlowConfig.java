package com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate;

import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateFlowEvent.FREEIPA_PROVIDER_TEMPLATE_UPDATE_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateFlowEvent.FREEIPA_PROVIDER_TEMPLATE_UPDATE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateFlowEvent.FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateFlowEvent.FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateFlowEvent.FREEIPA_PROVIDER_TEMPLATE_UPDATE_TRIGGER_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateState.FREEIPA_PROVIDER_TEMPLATE_UPDATE_FAIL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateState.FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateState.FREEIPA_PROVIDER_TEMPLATE_UPDATE_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class FreeIpaProviderTemplateUpdateFlowConfig extends StackStatusFinalizerAbstractFlowConfig<FreeIpaProviderTemplateUpdateState,
        FreeIpaProviderTemplateUpdateFlowEvent> {

    private static final List<Transition<FreeIpaProviderTemplateUpdateState, FreeIpaProviderTemplateUpdateFlowEvent>> TRANSITIONS =
            new Transition.Builder<FreeIpaProviderTemplateUpdateState, FreeIpaProviderTemplateUpdateFlowEvent>()
                    .defaultFailureEvent(FREEIPA_PROVIDER_TEMPLATE_UPDATE_FAILURE_EVENT)

                    .from(INIT_STATE).to(FREEIPA_PROVIDER_TEMPLATE_UPDATE_STATE)
                    .event(FREEIPA_PROVIDER_TEMPLATE_UPDATE_TRIGGER_EVENT)
                    .defaultFailureEvent()

                    .from(FREEIPA_PROVIDER_TEMPLATE_UPDATE_STATE).to(FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINISHED_STATE)
                    .event(FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINISHED_STATE).to(FINAL_STATE)
                    .event(FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<FreeIpaProviderTemplateUpdateState, FreeIpaProviderTemplateUpdateFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, FREEIPA_PROVIDER_TEMPLATE_UPDATE_FAIL_STATE, FREEIPA_PROVIDER_TEMPLATE_UPDATE_FAIL_HANDLED_EVENT);

    public FreeIpaProviderTemplateUpdateFlowConfig() {
        super(FreeIpaProviderTemplateUpdateState.class, FreeIpaProviderTemplateUpdateFlowEvent.class);
    }

    @Override
    protected List<Transition<FreeIpaProviderTemplateUpdateState, FreeIpaProviderTemplateUpdateFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaProviderTemplateUpdateState, FreeIpaProviderTemplateUpdateFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIpaProviderTemplateUpdateFlowEvent[] getEvents() {
        return FreeIpaProviderTemplateUpdateFlowEvent.values();
    }

    @Override
    public FreeIpaProviderTemplateUpdateFlowEvent[] getInitEvents() {
        return new FreeIpaProviderTemplateUpdateFlowEvent[] {FREEIPA_PROVIDER_TEMPLATE_UPDATE_TRIGGER_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Update AWS Launch Template";
    }
}
