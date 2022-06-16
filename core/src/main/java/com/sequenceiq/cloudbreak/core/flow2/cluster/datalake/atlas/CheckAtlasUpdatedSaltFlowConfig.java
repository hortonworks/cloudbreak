package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas.CheckAtlasUpdatedSaltEvent.CHECK_ATLAS_UPDATED_SALT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas.CheckAtlasUpdatedSaltEvent.CHECK_ATLAS_UPDATED_SALT_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas.CheckAtlasUpdatedSaltEvent.CHECK_ATLAS_UPDATED_SALT_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas.CheckAtlasUpdatedSaltEvent.CHECK_ATLAS_UPDATED_SALT_SUCCESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas.CheckAtlasUpdatedSaltEvent.CHECK_ATLAS_UPDATED_SALT_SUCCESS_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas.CheckAtlasUpdatedSaltState.CHECK_ATLAS_UPDATED_SALT_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas.CheckAtlasUpdatedSaltState.CHECK_ATLAS_UPDATED_SALT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas.CheckAtlasUpdatedSaltState.CHECK_ATLAS_UPDATED_SALT_SUCCESS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas.CheckAtlasUpdatedSaltState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas.CheckAtlasUpdatedSaltState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class CheckAtlasUpdatedSaltFlowConfig extends StackStatusFinalizerAbstractFlowConfig<CheckAtlasUpdatedSaltState, CheckAtlasUpdatedSaltEvent>
        implements RetryableFlowConfiguration<CheckAtlasUpdatedSaltEvent> {
    private static final List<Transition<CheckAtlasUpdatedSaltState, CheckAtlasUpdatedSaltEvent>> TRANSITIONS =
        new Builder<CheckAtlasUpdatedSaltState, CheckAtlasUpdatedSaltEvent>()
            .defaultFailureEvent(CHECK_ATLAS_UPDATED_SALT_FAILED_EVENT)

                .from(INIT_STATE)
                .to(CHECK_ATLAS_UPDATED_SALT_STATE)
                .event(CHECK_ATLAS_UPDATED_SALT_EVENT).noFailureEvent()

                .from(CHECK_ATLAS_UPDATED_SALT_STATE)
                .to(CHECK_ATLAS_UPDATED_SALT_SUCCESS_STATE)
                .event(CHECK_ATLAS_UPDATED_SALT_SUCCESS_EVENT).defaultFailureEvent()

                .from(CHECK_ATLAS_UPDATED_SALT_SUCCESS_STATE)
                .to(FINAL_STATE)
                .event(CHECK_ATLAS_UPDATED_SALT_SUCCESS_HANDLED_EVENT).defaultFailureEvent()

                .from(CHECK_ATLAS_UPDATED_SALT_FAILED_STATE)
                .to(FINAL_STATE)
                .event(CHECK_ATLAS_UPDATED_SALT_FAILURE_HANDLED_EVENT).noFailureEvent()

                .build();

    private static final FlowEdgeConfig<CheckAtlasUpdatedSaltState, CheckAtlasUpdatedSaltEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(
                    INIT_STATE, FINAL_STATE, CHECK_ATLAS_UPDATED_SALT_FAILED_STATE,
                    CHECK_ATLAS_UPDATED_SALT_FAILURE_HANDLED_EVENT
            );

    public CheckAtlasUpdatedSaltFlowConfig() {
        super(CheckAtlasUpdatedSaltState.class, CheckAtlasUpdatedSaltEvent.class);
    }

    @Override
    public CheckAtlasUpdatedSaltEvent[] getEvents() {
        return CheckAtlasUpdatedSaltEvent.values();
    }

    @Override
    public CheckAtlasUpdatedSaltEvent[] getInitEvents() {
        return new CheckAtlasUpdatedSaltEvent[] {
                CHECK_ATLAS_UPDATED_SALT_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Check atlas updated";
    }

    @Override
    protected List<Transition<CheckAtlasUpdatedSaltState, CheckAtlasUpdatedSaltEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<CheckAtlasUpdatedSaltState, CheckAtlasUpdatedSaltEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public CheckAtlasUpdatedSaltEvent getRetryableEvent() {
        return CHECK_ATLAS_UPDATED_SALT_FAILURE_HANDLED_EVENT;
    }
}
