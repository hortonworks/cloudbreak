package com.sequenceiq.datalake.flow.upgrade;

import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeEvent.SDX_IMAGE_CHANGED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeEvent.SDX_UPGRADE_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeEvent.SDX_UPGRADE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeEvent.SDX_UPGRADE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeEvent.SDX_UPGRADE_FAILED_TO_START_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeEvent.SDX_UPGRADE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeEvent.SDX_UPGRADE_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeEvent.SDX_UPGRADE_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeState.INIT_STATE;
import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeState.SDX_IMAGE_CHANGED_STATE;
import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeState.SDX_UPGRADE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeState.SDX_UPGRADE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeState.SDX_UPGRADE_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.upgrade.SdxUpgradeState.SDX_UPGRADE_START_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class SdxUpgradeFlowConfig extends AbstractFlowConfiguration<SdxUpgradeState, SdxUpgradeEvent> {

    private static final List<Transition<SdxUpgradeState, SdxUpgradeEvent>> TRANSITIONS =
            new Transition.Builder<SdxUpgradeState, SdxUpgradeEvent>()
                    .defaultFailureEvent(SDX_UPGRADE_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(SDX_UPGRADE_START_STATE)
                    .event(SDX_UPGRADE_EVENT).noFailureEvent()

                    .from(SDX_UPGRADE_START_STATE)
                    .to(SDX_IMAGE_CHANGED_STATE)
                    .event(SDX_IMAGE_CHANGED_EVENT).defaultFailureEvent()

                    .from(SDX_IMAGE_CHANGED_STATE)
                    .to(SDX_UPGRADE_IN_PROGRESS_STATE)
                    .event(SDX_UPGRADE_IN_PROGRESS_EVENT).defaultFailureEvent()

                    .from(SDX_UPGRADE_START_STATE)
                    .to(FINAL_STATE)
                    .event(SDX_UPGRADE_FAILED_TO_START_EVENT).defaultFailureEvent()

                    .from(SDX_UPGRADE_IN_PROGRESS_STATE)
                    .to(SDX_UPGRADE_FINISHED_STATE)
                    .event(SDX_UPGRADE_SUCCESS_EVENT).defaultFailureEvent()

                    .from(SDX_UPGRADE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(SDX_UPGRADE_FINALIZED_EVENT).defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<SdxUpgradeState, SdxUpgradeEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SDX_UPGRADE_FAILED_STATE, SDX_UPGRADE_FAILED_HANDLED_EVENT);

    public SdxUpgradeFlowConfig() {
        super(SdxUpgradeState.class, SdxUpgradeEvent.class);
    }

    @Override
    public SdxUpgradeEvent[] getEvents() {
        return SdxUpgradeEvent.values();
    }

    @Override
    public SdxUpgradeEvent[] getInitEvents() {
        return new SdxUpgradeEvent[]{
                SDX_UPGRADE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Upgrade SDX";
    }

    @Override
    protected List<Transition<SdxUpgradeState, SdxUpgradeEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<SdxUpgradeState, SdxUpgradeEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
