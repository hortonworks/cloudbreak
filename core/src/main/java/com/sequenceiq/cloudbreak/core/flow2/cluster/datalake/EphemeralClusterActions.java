package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake;

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
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClusterUpdateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClusterUpdateSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClusterUpdateTriggerEvent;

@Configuration
public class EphemeralClusterActions {

    @Inject
    private EphemeralClusterService ephemeralClusterService;

    @Bean(name = "EPHEMERAL_CLUSTER_UPDATE_STATE")
    public Action updateNameserverAction() {
        return new AbstractClusterAction<EphemeralClusterUpdateTriggerEvent>(EphemeralClusterUpdateTriggerEvent.class) {
            @Override
            protected void doExecute(ClusterContext context, EphemeralClusterUpdateTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                Stack stack = context.getStack();
                ephemeralClusterService.updateClusterStarted(stack);
                EphemeralClusterUpdateRequest request = new EphemeralClusterUpdateRequest(stack.getId());
                sendEvent(context.getFlowId(), request.selector(), request);
            }
        };
    }

    @Bean(name = "EPHEMERAL_CLUSTER_UPDATE_FINISHED_STATE")
    public Action ephemeralUpdateFinishedAction() {
        return new AbstractClusterAction<EphemeralClusterUpdateSuccess>(EphemeralClusterUpdateSuccess.class) {
            @Override
            protected void doExecute(ClusterContext context, EphemeralClusterUpdateSuccess payload, Map<Object, Object> variables) throws Exception {
                ephemeralClusterService.updateClusterFinished(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new StackEvent(EphemeralClusterEvent.EPHEMERAL_CLUSTER_FLOW_FINISHED.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "EPHEMERAL_CLUSTER_UPDATE_FAILED_STATE")
    public Action ephemeralUpdateFailedAction() {
        return new AbstractStackFailureAction<EphemeralClusterState, EphemeralClusterEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                ephemeralClusterService.updateClusterFailed(context.getStack(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(EphemeralClusterEvent.EPHEMERAL_CLUSTER_FAILURE_HANDLED.event(), context.getStack().getId());
            }
        };
    }

}
