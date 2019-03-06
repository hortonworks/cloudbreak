package com.sequenceiq.cloudbreak.core.flow2.cluster.stop;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopResult;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;

@Configuration
public class ClusterStopActions {

    @Inject
    private ClusterStopService clusterStopService;

    @Bean(name = "CLUSTER_STOPPING_STATE")
    public Action<?, ?> stoppingCluster() {
        return new AbstractClusterAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StackEvent payload, Map<Object, Object> variables) {
                clusterStopService.stoppingCluster(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new ClusterStopRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_STOP_FINISHED_STATE")
    public Action<?, ?> clusterStopFinished() {
        return new AbstractClusterAction<>(ClusterStopResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ClusterStopResult payload, Map<Object, Object> variables) {
                clusterStopService.clusterStopFinished(context.getStackId());
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_STOP_SUCCESSFUL, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(ClusterStopEvent.FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_STOP_FAILED_STATE")
    public Action<?, ?> clusterStopFailedAction() {
        return new AbstractStackFailureAction<ClusterStopState, ClusterStopEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                clusterStopService.handleClusterStopFailure(context.getStackView(), payload.getException().getMessage());
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_STOP_FAILED, context.getStackView());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterStopEvent.FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}
