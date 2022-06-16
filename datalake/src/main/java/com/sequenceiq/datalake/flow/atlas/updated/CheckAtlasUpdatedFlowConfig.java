package com.sequenceiq.datalake.flow.atlas.updated;

import static com.sequenceiq.datalake.flow.atlas.updated.CheckAtlasUpdatedEvent.CHECK_ATLAS_UPDATED_EVENT;
import static com.sequenceiq.datalake.flow.atlas.updated.CheckAtlasUpdatedEvent.CHECK_ATLAS_UPDATED_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.atlas.updated.CheckAtlasUpdatedEvent.CHECK_ATLAS_UPDATED_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.atlas.updated.CheckAtlasUpdatedEvent.CHECK_ATLAS_UPDATED_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.atlas.updated.CheckAtlasUpdatedState.CHECK_ATLAS_UPDATED_FAILED_STATE;
import static com.sequenceiq.datalake.flow.atlas.updated.CheckAtlasUpdatedState.CHECK_ATLAS_UPDATED_STATE;
import static com.sequenceiq.datalake.flow.atlas.updated.CheckAtlasUpdatedState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.atlas.updated.CheckAtlasUpdatedState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class CheckAtlasUpdatedFlowConfig
        extends AbstractFlowConfiguration<CheckAtlasUpdatedState, CheckAtlasUpdatedEvent>
        implements RetryableFlowConfiguration<CheckAtlasUpdatedEvent> {
    private static final List<Transition<CheckAtlasUpdatedState, CheckAtlasUpdatedEvent>> TRANSITIONS =
            new Builder<CheckAtlasUpdatedState, CheckAtlasUpdatedEvent>()
                    .defaultFailureEvent(CHECK_ATLAS_UPDATED_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(CHECK_ATLAS_UPDATED_STATE)
                    .event(CHECK_ATLAS_UPDATED_EVENT).noFailureEvent()

                    .from(CHECK_ATLAS_UPDATED_STATE)
                    .to(FINAL_STATE)
                    .event(CHECK_ATLAS_UPDATED_SUCCESS_EVENT).defaultFailureEvent()

                    .from(CHECK_ATLAS_UPDATED_FAILED_STATE)
                    .to(FINAL_STATE)
                    .event(CHECK_ATLAS_UPDATED_FAILURE_HANDLED_EVENT).noFailureEvent()

                    .build();

    private static final FlowEdgeConfig<CheckAtlasUpdatedState, CheckAtlasUpdatedEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, CHECK_ATLAS_UPDATED_FAILED_STATE, CHECK_ATLAS_UPDATED_FAILURE_HANDLED_EVENT);

    public CheckAtlasUpdatedFlowConfig() {
        super(CheckAtlasUpdatedState.class, CheckAtlasUpdatedEvent.class);
    }

    @Override
    public CheckAtlasUpdatedEvent[] getEvents() {
        return CheckAtlasUpdatedEvent.values();
    }

    @Override
    public CheckAtlasUpdatedEvent[] getInitEvents() {
        return new CheckAtlasUpdatedEvent[] {
                CHECK_ATLAS_UPDATED_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Check atlas updated";
    }

    @Override
    protected List<Transition<CheckAtlasUpdatedState, CheckAtlasUpdatedEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<CheckAtlasUpdatedState, CheckAtlasUpdatedEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public CheckAtlasUpdatedEvent getRetryableEvent() {
        return CHECK_ATLAS_UPDATED_FAILURE_HANDLED_EVENT;
    }
}
