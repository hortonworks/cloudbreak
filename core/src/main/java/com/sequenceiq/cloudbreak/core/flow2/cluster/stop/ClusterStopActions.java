package com.sequenceiq.cloudbreak.core.flow2.cluster.stop;


import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.AbstractClusterStartStopAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartStopContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterStopRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterStopResult;

@Configuration
public class ClusterStopActions {
    private static final String STACK_STATUS = "STACK_STATUS";
    @Inject
    private ClusterStopService clusterStopService;

    @Bean(name = "CLUSTER_STOP_STATE")
    public Action stopCluster() {
        return new AbstractClusterStartStopAction<StackStatusUpdateContext>(StackStatusUpdateContext.class) {
            @Override
            protected void doExecute(ClusterStartStopContext context, StackStatusUpdateContext payload, Map<Object, Object> variables) throws Exception {
                Stack stack = context.getStack();
                variables.put(STACK_STATUS, stack.getStatus());
                clusterStopService.stopCluster(stack);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterStartStopContext context) {
                return new ClusterStopRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_STOP_FINISHED_STATE")
    public Action finishStopCluster() {
        return new AbstractClusterStartStopAction<ClusterStopResult>(ClusterStopResult.class) {
            @Override
            protected void doExecute(ClusterStartStopContext context, ClusterStopResult payload, Map<Object, Object> variables) throws Exception {
                Status statusBeforeAmbariStop = (Status) variables.get(STACK_STATUS);
                clusterStopService.finishStopCluster(context.getStack(), statusBeforeAmbariStop);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterStartStopContext context) {
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
