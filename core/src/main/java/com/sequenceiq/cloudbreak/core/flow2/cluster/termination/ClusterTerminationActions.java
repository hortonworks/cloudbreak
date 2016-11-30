package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult;

@Configuration
public class ClusterTerminationActions {
    @Inject
    private ClusterTerminationFlowService clusterTerminationFlowService;

    @Bean(name = "CLUSTER_TERMINATING_STATE")
    public Action terminatingCluster() {
        return new AbstractClusterAction<StackEvent>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                clusterTerminationFlowService.terminateCluster(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new ClusterTerminationRequest(context.getStack().getId(), context.getCluster().getId());

            }
        };
    }

    @Bean(name = "CLUSTER_TERMINATION_FINISH_STATE")
    public Action clusterTerminationFinished() {
        return new AbstractClusterAction<ClusterTerminationResult>(ClusterTerminationResult.class) {
            @Override
            protected void doExecute(ClusterContext context, ClusterTerminationResult payload, Map<Object, Object> variables) throws Exception {
                if (payload.isOperationAllowed()) {
                    clusterTerminationFlowService.finishClusterTerminationAllowed(context, payload);
                } else {
                    clusterTerminationFlowService.finishClusterTerminationNotAllowed(context, payload);
                }
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new StackEvent(ClusterTerminationEvent.FINALIZED_EVENT.stringRepresentation(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_TERMINATION_FAILED_STATE")
    public Action clusterTerminationFailedAction() {
        return new AbstractStackFailureAction<ClusterTerminationState, ClusterTerminationEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                clusterTerminationFlowService.handleClusterTerminationError(payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterTerminationEvent.FAIL_HANDLED_EVENT.stringRepresentation(), context.getStack().getId());
            }
        };
    }


}
