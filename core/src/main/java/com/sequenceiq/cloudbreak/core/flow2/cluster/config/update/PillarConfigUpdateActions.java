package com.sequenceiq.cloudbreak.core.flow2.cluster.config.update;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigurationUpdateEvent.PILLAR_CONFIG_UPDATE_FINALIZE_EVENT;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event.PillarConfigUpdateRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event.PillarConfigUpdateSuccess;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import java.util.Map;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

@Configuration
public class PillarConfigUpdateActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(PillarConfigUpdateActions.class);

    @Inject
    private PillarConfigUpdateService pillarConfigUpdateService;

    @Bean(name = "PILLAR_CONFIG_UPDATE_START_STATE")
    public Action<?, ?> configUpdateStartAction() {
        return new AbstractClusterAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StackEvent payload,
                Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new PillarConfigUpdateRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "PILLAR_CONFIG_UPDATE_FINISHED_STATE")
    public Action<?, ?> configUpdateFinishedAction() {
        return new AbstractClusterAction<>(PillarConfigUpdateSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, PillarConfigUpdateSuccess payload,
                Map<Object, Object> variables) {
                pillarConfigUpdateService.configUpdateFinished(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(PILLAR_CONFIG_UPDATE_FINALIZE_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "PILLAR_CONFIG_UPDATE_FAILED_STATE")
    public Action<?, ?> configUpdateFailedAction() {
        return new AbstractStackFailureAction<ClusterCreationState, ClusterCreationEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload,
                Map<Object, Object> variables) {
                LOGGER.warn("Pillar configuration update failed.", payload.getException());
                pillarConfigUpdateService
                    .handleConfigUpdateFailure(context.getStackView(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(PillarConfigurationUpdateEvent.PILLAR_CONFIG_UPDATE_FAILURE_HANDLED_EVENT
                    .event(),
                    context.getStackView().getId());
            }
        };
    }

}
