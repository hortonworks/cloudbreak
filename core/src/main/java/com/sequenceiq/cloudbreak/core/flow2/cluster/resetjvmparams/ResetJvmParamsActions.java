package com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RESET_JVM_PARAMS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RESET_JVM_PARAMS_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RESET_JVM_PARAMS_FINISHED;

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
import com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.handler.ResetJvmParamsRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.handler.ResetJvmParamsResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Configuration
public class ResetJvmParamsActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResetJvmParamsActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Bean(name = "RESET_JVM_PARAMS_STATE")
    public Action<?, ?> resetJvmParams() {
        return new AbstractClusterAction<>(ResetJvmParamsTriggerEvent.class) {

            @Override
            protected void doExecute(ClusterViewContext context, ResetJvmParamsTriggerEvent payload, Map<Object, Object> variables) {
                ResetJvmParamsRequest resetJvmParamsRequest = new ResetJvmParamsRequest(payload.getResourceId());
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.RESET_JVM_PARAMS, "Resetting JVM params");
                flowMessageService.fireEventAndLog(payload.getResourceId(), UPDATE_IN_PROGRESS.name(), CLUSTER_RESET_JVM_PARAMS);
                sendEvent(context, resetJvmParamsRequest);
            }
        };
    }

    @Bean(name = "RESET_JVM_PARAMS_FINISHED_STATE")
    public Action<?, ?> resetJvmParamsFinished() {
        return new AbstractClusterAction<>(ResetJvmParamsResult.class) {

            @Override
            protected void doExecute(ClusterViewContext context, ResetJvmParamsResult payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.AVAILABLE);
                flowMessageService.fireEventAndLog(payload.getResourceId(), UPDATE_IN_PROGRESS.name(), CLUSTER_RESET_JVM_PARAMS_FINISHED);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(ResetJvmParamsFlowEvent.RESET_JVM_PARAMS_FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "RESET_JVM_PARAMS_FAILED_STATE")
    public Action<?, ?> resetJvmParamsFailed() {
        return new AbstractStackFailureAction<ResetJvmParamsFlowState, ResetJvmParamsFlowEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.warn("Reset JVM params failed: {}", payload.getException().getMessage());
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.RESET_JVM_PARAMS_FAILED);
                flowMessageService.fireEventAndLog(payload.getResourceId(), AVAILABLE.name(), CLUSTER_RESET_JVM_PARAMS_FAILED,
                        payload.getException().getMessage());
                sendEvent(context, ResetJvmParamsFlowEvent.RESET_JVM_PARAMS_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }
}
