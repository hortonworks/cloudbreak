package com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_SERVER_RESTARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_SERVER_RESTARTING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_SERVER_RESTART_FAILED;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RestartClusterManagerServerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RestartClusterManagerServerSuccess;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Configuration
public class RestartClusterManagerActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestartClusterManagerActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Bean(name = "RESTART_CLUSTER_MANAGER_FLOW_STATE")
    public Action<?, ?> restartClusterManager() {
        return new AbstractClusterAction<>(StackEvent.class) {

            @Override
            protected void doExecute(ClusterViewContext context, StackEvent payload, Map<Object, Object> variables) {
                RestartClusterManagerServerRequest restartClusterManagerRequest = new RestartClusterManagerServerRequest(payload.getResourceId(),
                        false, RESTART_CLUSTER_MANAGER_FAILURE_EVENT.event());
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.CLUSTER_MANAGER_RESTART_IN_PROGRESS,
                        "Restarting Cluster Manager");
                flowMessageService.fireEventAndLog(payload.getResourceId(), UPDATE_IN_PROGRESS.name(), CLUSTER_MANAGER_SERVER_RESTARTING);
                sendEvent(context, restartClusterManagerRequest);
            }
        };
    }

    @Bean(name = "RESTART_CLUSTER_MANAGER_FINISED_STATE")
    public Action<?, ?> restartClusterManagerFinished() {
        return new AbstractClusterAction<>(RestartClusterManagerServerSuccess.class) {

            @Override
            protected void doExecute(ClusterViewContext context, RestartClusterManagerServerSuccess payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.AVAILABLE);
                flowMessageService.fireEventAndLog(payload.getResourceId(), AVAILABLE.name(), CLUSTER_MANAGER_SERVER_RESTARTED);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(RESTART_CLUSTER_MANAGER_FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "RESTART_CLUSTER_MANAGER_FAILED_STATE")
    public Action<?, ?> restartClusterManagerFailedAction() {
        return new AbstractStackFailureAction<RestartClusterManagerFlowState, RestartClusterManagerFlowEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.warn("Restarting Cluster Manager failed: {}", payload.getException().getMessage());
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.CLUSTER_MANAGER_RESTART_FAILED);
                flowMessageService.fireEventAndLog(payload.getResourceId(), AVAILABLE.name(), CLUSTER_MANAGER_SERVER_RESTART_FAILED);
                sendEvent(context, RESTART_CLUSTER_MANAGER_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }

}
