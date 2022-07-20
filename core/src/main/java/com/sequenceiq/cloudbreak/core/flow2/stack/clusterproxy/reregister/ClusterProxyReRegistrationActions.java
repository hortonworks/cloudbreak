package com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister;

import static com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister.ClusterProxyReRegistrationEvent.CLUSTER_PROXY_RE_REGISTRATION_FINISHED_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyReRegistrationRequest;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class ClusterProxyReRegistrationActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyReRegistrationActions.class);

    @Inject
    private StackService stackService;

    @Bean(name = "CLUSTER_PROXY_RE_REGISTRATION_STATE")
    public Action<?, ?> reRegisterClusterProxyConfigAction() {
        return new AbstractStackAction<ClusterProxyReRegistrationState, ClusterProxyReRegistrationEvent, StackContext, StackEvent>(StackEvent.class) {
            @Override
            protected StackContext createFlowContext(FlowParameters flowParameters,
                    StateContext<ClusterProxyReRegistrationState, ClusterProxyReRegistrationEvent> stateContext, StackEvent payload) {
                return new StackContext(flowParameters, stackService.getById(payload.getResourceId()), null, null, null);
            }

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new ClusterProxyReRegistrationRequest(
                        context.getStack().getId(), context.getStack().getCloudPlatform(),
                        CLUSTER_PROXY_RE_REGISTRATION_FINISHED_EVENT.event()
                );
            }

            @Override
            protected Object getFailurePayload(StackEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new StackFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "CLUSTER_PROXY_RE_REGISTRATION_FAILED_STATE")
    public Action<?, ?> reRegisterClusterProxyConfigFailedAction() {
        return new AbstractStackFailureAction<ClusterProxyReRegistrationState, ClusterProxyReRegistrationEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error(
                        "Failed to re-register the cluster proxy config for stack with ID: " + context.getStackId(),
                        payload.getException()
                );
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(
                        ClusterProxyReRegistrationEvent.CLUSTER_PROXY_RE_REGISTRATION_FAIL_HANDLED_EVENT.event(), context.getStackId()
                );
            }
        };
    }
}
