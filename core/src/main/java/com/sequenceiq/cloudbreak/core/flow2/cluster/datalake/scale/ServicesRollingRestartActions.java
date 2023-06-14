package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale.ServicesRollingRestartEvent.SERVICES_ROLLING_RESTART_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale.ServicesRollingRestartEvent.SERVICES_ROLLING_RESTART_IN_PROGRESS_EVENT;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.Flow;

@Configuration
public class ServicesRollingRestartActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicesRollingRestartActions.class);

    @Bean("ROLLING_RESTART_STATE")
    public Action<?, ?> rollingRestartServices() {
        return new AbstractClusterAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StackEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Services rolling restart started for stack: {}", payload.getResourceId());
                sendEvent(context, SERVICES_ROLLING_RESTART_IN_PROGRESS_EVENT.event(), payload);
            }
        };
    }

    @Bean("ROLLING_RESTART_FINISHED_STATE")
    public Action<?, ?> rollingRestartFinished() {
        return new AbstractClusterAction<>(ClusterServicesRestartEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ClusterServicesRestartEvent payload, Map<Object, Object> variables) {
                LOGGER.info("ServicesRollingRestartFinished for stack: {}", payload.getResourceId());
                sendEvent(context, SERVICES_ROLLING_RESTART_FINISHED_EVENT.event(), payload);
            }
        };
    }

    @Bean("ROLLING_RESTART_FAILED_STATE")
    public Action<?, ?> rollingRestartFailed() {
        return new AbstractClusterAction<>(ClusterServicesRestartEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ClusterServicesRestartEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Services rolling restart failed with exception");
                Flow flow = getFlow(context.getFlowParameters().getFlowId());
                flow.setFlowFailed(payload.getException());
            }
        };
    }
}
