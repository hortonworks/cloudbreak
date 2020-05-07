package com.sequenceiq.datalake.flow.upgrade;

import static com.sequenceiq.datalake.flow.upgrade.SdxOsUpgradeEvent.SDX_IMAGE_CHANGED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxOsUpgradeEvent.SDX_UPGRADE_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxOsUpgradeEvent.SDX_UPGRADE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxOsUpgradeEvent.SDX_UPGRADE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxOsUpgradeEvent.SDX_UPGRADE_FAILED_TO_START_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxOsUpgradeEvent.SDX_UPGRADE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxOsUpgradeEvent.SDX_UPGRADE_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxOsUpgradeEvent.SDX_UPGRADE_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxOsUpgradeState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.upgrade.SdxOsUpgradeState.INIT_STATE;
import static com.sequenceiq.datalake.flow.upgrade.SdxOsUpgradeState.SDX_IMAGE_CHANGED_STATE;
import static com.sequenceiq.datalake.flow.upgrade.SdxOsUpgradeState.SDX_UPGRADE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.upgrade.SdxOsUpgradeState.SDX_UPGRADE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.upgrade.SdxOsUpgradeState.SDX_UPGRADE_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.upgrade.SdxOsUpgradeState.SDX_UPGRADE_START_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class SdxOsUpgradeFlowConfig extends AbstractFlowConfiguration<SdxOsUpgradeState, SdxOsUpgradeEvent> implements
        RetryableFlowConfiguration<SdxOsUpgradeEvent> {

    private static final List<Transition<SdxOsUpgradeState, SdxOsUpgradeEvent>> TRANSITIONS =
            new Transition.Builder<SdxOsUpgradeState, SdxOsUpgradeEvent>()
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

    private static final FlowEdgeConfig<SdxOsUpgradeState, SdxOsUpgradeEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SDX_UPGRADE_FAILED_STATE, SDX_UPGRADE_FAILED_HANDLED_EVENT);

    public SdxOsUpgradeFlowConfig() {
        super(SdxOsUpgradeState.class, SdxOsUpgradeEvent.class);
    }

    @Override
    public SdxOsUpgradeEvent[] getEvents() {
        return SdxOsUpgradeEvent.values();
    }

    @Override
    public SdxOsUpgradeEvent[] getInitEvents() {
        return new SdxOsUpgradeEvent[]{
                SDX_UPGRADE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Upgrade SDX";
    }

    @Override
    protected List<Transition<SdxOsUpgradeState, SdxOsUpgradeEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<SdxOsUpgradeState, SdxOsUpgradeEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SdxOsUpgradeEvent getRetryableEvent() {
        return SDX_UPGRADE_FAILED_HANDLED_EVENT;
    }
}
