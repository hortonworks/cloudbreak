package com.sequenceiq.cloudbreak.core.flow2.cluster.stop;


import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopResult;

@Configuration
public class ClusterStopActions {
    private static final String STACK_STATUS = "STACK_STATUS";
    @Inject
    private ClusterStopService clusterStopService;

    @Bean(name = "CLUSTER_STOPPING_STATE")
    public Action stoppingCluster() {
        return new AbstractClusterAction<StackEvent>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                Stack stack = context.getStack();
                variables.put(STACK_STATUS, stack.getStatus());
                clusterStopService.stoppingCluster(stack);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new ClusterStopRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_STOP_FINISHED_STATE")
    public Action clusterStopFinished() {
        return new AbstractClusterAction<ClusterStopResult>(ClusterStopResult.class) {
            @Override
            protected void doExecute(ClusterContext context, ClusterStopResult payload, Map<Object, Object> variables) throws Exception {
                Status statusBeforeAmbariStop = (Status) variables.get(STACK_STATUS);
                clusterStopService.clusterStopFinished(context.getStack(), statusBeforeAmbariStop);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new StackEvent(ClusterStopEvent.FINALIZED_EVENT.stringRepresentation(), context.getStack().getId());
            }
        };
    }


    @Bean(name = "CLUSTER_STOP_FAILED_STATE")
    public Action clusterStopFailedAction() {
        return new AbstractStackFailureAction<ClusterStopState, ClusterStopEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                clusterStopService.handleClusterStopFailure(context.getStack(), payload.getException().getMessage());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterStopEvent.FAIL_HANDLED_EVENT.stringRepresentation(), context.getStack().getId());
            }
        };
    }
}
