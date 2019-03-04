package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake;

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
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClusterUpdateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClusterUpdateSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClusterUpdateTriggerEvent;

@Configuration
public class EphemeralClusterActions {

    @Inject
    private EphemeralClusterService ephemeralClusterService;

    @Bean(name = "EPHEMERAL_CLUSTER_UPDATE_STATE")
    public Action<?, ?> updateNameserverAction() {
        return new AbstractClusterAction<>(EphemeralClusterUpdateTriggerEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, EphemeralClusterUpdateTriggerEvent payload, Map<Object, Object> variables) {
                ephemeralClusterService.updateClusterStarted(context.getStackId());
                Selectable request = new EphemeralClusterUpdateRequest(context.getStackId());
                sendEvent(context.getFlowId(), request.selector(), request);
            }
        };
    }

    @Bean(name = "EPHEMERAL_CLUSTER_UPDATE_FINISHED_STATE")
    public Action<?, ?> ephemeralUpdateFinishedAction() {
        return new AbstractClusterAction<>(EphemeralClusterUpdateSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, EphemeralClusterUpdateSuccess payload, Map<Object, Object> variables) {
                ephemeralClusterService.updateClusterFinished(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(EphemeralClusterEvent.EPHEMERAL_CLUSTER_FLOW_FINISHED.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "EPHEMERAL_CLUSTER_UPDATE_FAILED_STATE")
    public Action<?, ?> ephemeralUpdateFailedAction() {
        return new AbstractStackFailureAction<EphemeralClusterState, EphemeralClusterEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                ephemeralClusterService.updateClusterFailed(context.getStackView().getId(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(EphemeralClusterEvent.EPHEMERAL_CLUSTER_FAILURE_HANDLED.event(), context.getStackView().getId());
            }
        };
    }

}
