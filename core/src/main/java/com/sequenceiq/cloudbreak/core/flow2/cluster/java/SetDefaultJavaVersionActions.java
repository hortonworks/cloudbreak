package com.sequenceiq.cloudbreak.core.flow2.cluster.java;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SET_DEFAULT_JAVA_VERSION;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SET_DEFAULT_JAVA_VERSION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SET_DEFAULT_JAVA_VERSION_FINISHED;

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
import com.sequenceiq.cloudbreak.core.flow2.cluster.java.handler.SetDefaultJavaVersionRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.java.handler.SetDefaultJavaVersionResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Configuration
public class SetDefaultJavaVersionActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetDefaultJavaVersionActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Bean(name = "SET_DEFAULT_JAVA_VERSION_STATE")
    public Action<?, ?> setDefaultJavaVersion() {
        return new AbstractClusterAction<>(SetDefaultJavaVersionTriggerEvent.class) {

            @Override
            protected void doExecute(ClusterViewContext context, SetDefaultJavaVersionTriggerEvent payload, Map<Object, Object> variables) {
                SetDefaultJavaVersionRequest setDefaultJavaVersionRequest = new SetDefaultJavaVersionRequest(payload.getResourceId(),
                        payload.getDefaultJavaVersion());
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.SET_DEFAULT_JAVA_VERSION, "Setting default java version");
                flowMessageService.fireEventAndLog(payload.getResourceId(), UPDATE_IN_PROGRESS.name(), CLUSTER_SET_DEFAULT_JAVA_VERSION,
                        payload.getDefaultJavaVersion());
                sendEvent(context, setDefaultJavaVersionRequest);
            }
        };
    }

    @Bean(name = "SET_DEFAULT_JAVA_VERSION_FINISED_STATE")
    public Action<?, ?> setDefaultJavaVersionFinished() {
        return new AbstractClusterAction<>(SetDefaultJavaVersionResult.class) {

            @Override
            protected void doExecute(ClusterViewContext context, SetDefaultJavaVersionResult payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.AVAILABLE);
                flowMessageService.fireEventAndLog(payload.getResourceId(), UPDATE_IN_PROGRESS.name(), CLUSTER_SET_DEFAULT_JAVA_VERSION_FINISHED);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "SET_DEFAULT_JAVA_VERSION_FAILED_STATE")
    public Action<?, ?> clusterStopFailedAction() {
        return new AbstractStackFailureAction<SetDefaultJavaVersionFlowState, SetDefaultJavaVersionFlowEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.warn("Set default java version failed: {}", payload.getException().getMessage());
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.SET_DEFAULT_JAVA_VERSION_FAILED);
                flowMessageService.fireEventAndLog(payload.getResourceId(), AVAILABLE.name(), CLUSTER_SET_DEFAULT_JAVA_VERSION_FAILED);
                sendEvent(context, SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }
}
