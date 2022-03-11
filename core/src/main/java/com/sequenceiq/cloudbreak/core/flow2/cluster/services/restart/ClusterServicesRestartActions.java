package com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterServicesRestartPollingRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterServicesRestartPollingResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterServicesRestartRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterServicesRestartResult;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;

@Configuration
public class ClusterServicesRestartActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterServicesRestartActions.class);

    @Bean(name = "CLUSTER_SERVICE_RESTARTING_STATE")
    public Action<?, ?> startingCluster() {
        return new AbstractClusterAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StackEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new ClusterServicesRestartRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_SERVICE_RESTART_POLLING_STATE")
    public Action<?, ?> clusterStartPolling() {
        return new AbstractClusterAction<>(ClusterServicesRestartResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ClusterServicesRestartResult payload, Map<Object, Object> variables) {
                sendEvent(context, new ClusterServicesRestartPollingRequest(context.getStackId(), payload.getRequestId()));
            }
        };
    }

    @Bean(name = "CLUSTER_SERVICE_RESTART_FINISHED_STATE")
    public Action<?, ?> clusterStartFinished() {
        return new AbstractClusterAction<>(ClusterServicesRestartPollingResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ClusterServicesRestartPollingResult payload, Map<Object, Object> variables) {
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_START_SUCCESSFUL, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(ClusterServicesRestartEvent.FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_SERVICE_RESTART_FAILED_STATE")
    public Action<?, ?> clusterStartFailedAction() {
        return new AbstractStackFailureAction<ClusterServicesRestartState, ClusterServicesRestartEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterServicesRestartEvent.FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}
