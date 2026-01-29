package com.sequenceiq.datalake.flow.imdupdate;


import static com.sequenceiq.datalake.flow.imdupdate.SdxInstanceMetadataUpdateState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.imdupdate.SdxInstanceMetadataUpdateState.INIT_STATE;
import static com.sequenceiq.datalake.flow.imdupdate.SdxInstanceMetadataUpdateState.SDX_IMD_UPDATE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.imdupdate.SdxInstanceMetadataUpdateState.SDX_IMD_UPDATE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.imdupdate.SdxInstanceMetadataUpdateState.SDX_IMD_UPDATE_STATE;
import static com.sequenceiq.datalake.flow.imdupdate.SdxInstanceMetadataUpdateState.SDX_IMD_UPDATE_WAIT_STATE;
import static com.sequenceiq.datalake.flow.imdupdate.SdxInstanceMetadataUpdateStateSelectors.SDX_IMD_UPDATE_EVENT;
import static com.sequenceiq.datalake.flow.imdupdate.SdxInstanceMetadataUpdateStateSelectors.SDX_IMD_UPDATE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.imdupdate.SdxInstanceMetadataUpdateStateSelectors.SDX_IMD_UPDATE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.imdupdate.SdxInstanceMetadataUpdateStateSelectors.SDX_IMD_UPDATE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.imdupdate.SdxInstanceMetadataUpdateStateSelectors.SDX_IMD_UPDATE_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.imdupdate.SdxInstanceMetadataUpdateStateSelectors.SDX_IMD_UPDATE_WAIT_SUCCESS_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class SdxInstanceMetadataUpdateFlowConfig extends AbstractFlowConfiguration<SdxInstanceMetadataUpdateState, SdxInstanceMetadataUpdateStateSelectors>
        implements RetryableDatalakeFlowConfiguration<SdxInstanceMetadataUpdateStateSelectors> {

    private static final List<Transition<SdxInstanceMetadataUpdateState, SdxInstanceMetadataUpdateStateSelectors>> TRANSITIONS =
            new Transition.Builder<SdxInstanceMetadataUpdateState, SdxInstanceMetadataUpdateStateSelectors>()
                    .defaultFailureEvent(SDX_IMD_UPDATE_FAILED_EVENT)

                    .from(INIT_STATE).to(SDX_IMD_UPDATE_STATE)
                    .event(SDX_IMD_UPDATE_EVENT).defaultFailureEvent()

                    .from(SDX_IMD_UPDATE_STATE).to(SDX_IMD_UPDATE_WAIT_STATE)
                    .event(SDX_IMD_UPDATE_SUCCESS_EVENT).defaultFailureEvent()

                    .from(SDX_IMD_UPDATE_WAIT_STATE).to(SDX_IMD_UPDATE_FINISHED_STATE)
                    .event(SDX_IMD_UPDATE_WAIT_SUCCESS_EVENT).defaultFailureEvent()

                    .from(SDX_IMD_UPDATE_FINISHED_STATE).to(FINAL_STATE)
                    .event(SDX_IMD_UPDATE_FINALIZED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<SdxInstanceMetadataUpdateState, SdxInstanceMetadataUpdateStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SDX_IMD_UPDATE_FAILED_STATE, SDX_IMD_UPDATE_FAILED_HANDLED_EVENT);

    public SdxInstanceMetadataUpdateFlowConfig() {
        super(SdxInstanceMetadataUpdateState.class, SdxInstanceMetadataUpdateStateSelectors.class);
    }

    @Override
    public SdxInstanceMetadataUpdateStateSelectors[] getEvents() {
        return SdxInstanceMetadataUpdateStateSelectors.values();
    }

    @Override
    public SdxInstanceMetadataUpdateStateSelectors[] getInitEvents() {
        return new SdxInstanceMetadataUpdateStateSelectors[]{
                SDX_IMD_UPDATE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Update instance metadata";
    }

    @Override
    protected List<Transition<SdxInstanceMetadataUpdateState, SdxInstanceMetadataUpdateStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<SdxInstanceMetadataUpdateState, SdxInstanceMetadataUpdateStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SdxInstanceMetadataUpdateStateSelectors getRetryableEvent() {
        return SDX_IMD_UPDATE_FAILED_HANDLED_EVENT;
    }
}
