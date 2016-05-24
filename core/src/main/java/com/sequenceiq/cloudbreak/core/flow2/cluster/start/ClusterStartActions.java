package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterStartRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterStartResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Configuration
public class ClusterStartActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartActions.class);
    @Inject
    private ClusterService clusterService;
    @Inject
    private ClusterStartService clusterStartService;

    @Bean(name = "CLUSTER_START_STATE")
    public Action startCluster() {
        return new AbstractClusterStartStopAction<StackStatusUpdateContext>(StackStatusUpdateContext.class) {
            @Override
            protected void doExecute(ClusterStartStopContext context, StackStatusUpdateContext payload, Map<Object, Object> variables) throws Exception {
                Stack stack = context.getStack();
                Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
                MDCBuilder.buildMdcContext(stack);
                if (cluster != null && cluster.isStartRequested()) {
                    clusterStartService.startCluster(stack, cluster);
                    sendEvent(context);
                } else {
                    String message = "Cluster start has not been requested, start cluster later.";
                    LOGGER.info(message);
                    sendEvent(context.getFlowId(), ClusterStartEvent.FAILURE_EVENT.name(),
                            getFailurePayload(payload, Optional.of(context), new RuntimeException(message)));
                }
            }

            @Override
            protected Selectable createRequest(ClusterStartStopContext context) {
                return new ClusterStartRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_START_FINISHED_STATE")
    public Action finishStartCluster() {
        return new AbstractClusterStartStopAction<ClusterStartResult>(ClusterStartResult.class) {
            @Override
            protected void doExecute(ClusterStartStopContext context, ClusterStartResult payload, Map<Object, Object> variables) throws Exception {
                clusterStartService.finishStartCluster(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterStartStopContext context) {
                return new StackEvent(ClusterStartEvent.FINALIZED_EVENT.stringRepresentation(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_START_FAILED_STATE")
    public Action clusterStartFailedAction() {
        return new AbstractStackFailureAction<ClusterStartState, ClusterStartEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                clusterStartService.handleClusterStartFailure(context.getStack(), payload.getException().getMessage());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterStartEvent.FAIL_HANDLED_EVENT.stringRepresentation(), context.getStack().getId());
            }
        };
    }
}
