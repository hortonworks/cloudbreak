package com.sequenceiq.freeipa.flow.freeipa.imdupdate;

import static com.sequenceiq.freeipa.flow.freeipa.imdupdate.FreeIpaInstanceMetadataUpdateState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.imdupdate.FreeIpaInstanceMetadataUpdateState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.imdupdate.FreeIpaInstanceMetadataUpdateState.STACK_IMDUPDATE_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.imdupdate.FreeIpaInstanceMetadataUpdateState.STACK_IMDUPDATE_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.imdupdate.FreeIpaInstanceMetadataUpdateState.STACK_IMDUPDATE_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateEvent.STACK_IMDUPDATE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FINISHED_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateEvent;

@Component
public class FreeIpaInstanceMetadataUpdateFlowConfig extends AbstractFlowConfiguration<FreeIpaInstanceMetadataUpdateState, FreeIpaInstanceMetadataUpdateEvent> {
    private static final List<Transition<FreeIpaInstanceMetadataUpdateState, FreeIpaInstanceMetadataUpdateEvent>> TRANSITIONS =
            new Builder<FreeIpaInstanceMetadataUpdateState, FreeIpaInstanceMetadataUpdateEvent>()
                    .defaultFailureEvent(STACK_IMDUPDATE_FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(STACK_IMDUPDATE_STATE)
                    .event(STACK_IMDUPDATE_EVENT)
                    .defaultFailureEvent()

                    .from(STACK_IMDUPDATE_STATE)
                    .to(STACK_IMDUPDATE_FINISHED_STATE)
                    .event(STACK_IMDUPDATE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(STACK_IMDUPDATE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(STACK_IMDUPDATE_FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<FreeIpaInstanceMetadataUpdateState, FreeIpaInstanceMetadataUpdateEvent> EDGE_CONFIG = new FlowEdgeConfig<>(
            INIT_STATE,
            FINAL_STATE,
            STACK_IMDUPDATE_FAILED_STATE,
            STACK_IMDUPDATE_FAIL_HANDLED_EVENT);

    public FreeIpaInstanceMetadataUpdateFlowConfig() {
        super(FreeIpaInstanceMetadataUpdateState.class, FreeIpaInstanceMetadataUpdateEvent.class);
    }

    @Override
    protected List<Transition<FreeIpaInstanceMetadataUpdateState, FreeIpaInstanceMetadataUpdateEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaInstanceMetadataUpdateState, FreeIpaInstanceMetadataUpdateEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIpaInstanceMetadataUpdateEvent[] getEvents() {
        return FreeIpaInstanceMetadataUpdateEvent.values();
    }

    @Override
    public FreeIpaInstanceMetadataUpdateEvent[] getInitEvents() {
        return new FreeIpaInstanceMetadataUpdateEvent[] {
                STACK_IMDUPDATE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Instance metadata update on FreeIPAs instances";
    }

}
