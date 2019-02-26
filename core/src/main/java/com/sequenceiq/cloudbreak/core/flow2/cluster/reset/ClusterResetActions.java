package com.sequenceiq.cloudbreak.core.flow2.cluster.reset;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetResult;

@Configuration
public class ClusterResetActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterResetActions.class);

    @Inject
    private ClusterResetService clusterResetService;

    @Bean(name = "CLUSTER_RESET_STATE")
    public Action<?, ?> syncCluster() {
        return new AbstractClusterResetAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StackEvent payload, Map<Object, Object> variables) {
                clusterResetService.resetCluster(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new ClusterResetRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_RESET_FINISHED_STATE")
    public Action<?, ?> finishResetCluster() {
        return new AbstractClusterResetAction<>(ClusterResetResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ClusterResetResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StartClusterRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_RESET_START_AMBARI_FINISHED_STATE")
    public Action<?, ?> finishStartAmbari() {
        return new AbstractClusterResetAction<>(StartClusterSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StartClusterSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(ClusterResetEvent.FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_RESET_FAILED_STATE")
    public Action<?, ?> clusterResetFailedAction() {
        return new AbstractStackFailureAction<ClusterResetState, ClusterResetEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                clusterResetService.handleResetClusterFailure(context.getStackView(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterResetEvent.FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}
