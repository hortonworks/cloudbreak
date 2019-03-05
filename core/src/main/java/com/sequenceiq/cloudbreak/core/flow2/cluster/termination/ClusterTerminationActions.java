package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.DisableKerberosResultToStackEventConverter;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DisableKerberosRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.PrepareClusterTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.PrepareClusterTerminationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult;

@Configuration
public class ClusterTerminationActions {
    @Inject
    private ClusterTerminationFlowService clusterTerminationFlowService;

    @Bean(name = "PREPARE_CLUSTER_STATE")
    public Action<?, ?> prepareCluster() {
        return new AbstractClusterAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StackEvent payload, Map<Object, Object> variables) {
                clusterTerminationFlowService.terminateCluster(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new PrepareClusterTerminationRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "DISABLE_KERBEROS_STATE")
    public Action<?, ?> disableKerboros() {
        return new AbstractClusterAction<>(PrepareClusterTerminationResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, PrepareClusterTerminationResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new DisableKerberosRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_TERMINATING_STATE")
    public Action<?, ?> terminatingCluster() {
        return new AbstractClusterAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StackEvent payload, Map<Object, Object> variables) {
                clusterTerminationFlowService.terminateCluster(context);
                sendEvent(context);
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<StackEvent>> payloadConverters) {
                payloadConverters.add(new DisableKerberosResultToStackEventConverter());
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new ClusterTerminationRequest(context.getStackId(), context.getClusterView() != null ? context.getClusterView().getId() : null);
            }
        };
    }

    @Bean(name = "CLUSTER_TERMINATION_FINISH_STATE")
    public Action<?, ?> clusterTerminationFinished() {
        return new AbstractClusterAction<>(ClusterTerminationResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ClusterTerminationResult payload, Map<Object, Object> variables) {
                if (payload.isOperationAllowed()) {
                    clusterTerminationFlowService.finishClusterTerminationAllowed(context, payload);
                } else {
                    clusterTerminationFlowService.finishClusterTerminationNotAllowed(context, payload);
                }
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(ClusterTerminationEvent.FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_TERMINATION_FAILED_STATE")
    public Action<?, ?> clusterTerminationFailedAction() {
        return new AbstractStackFailureAction<ClusterTerminationState, ClusterTerminationEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                clusterTerminationFlowService.handleClusterTerminationError(payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterTerminationEvent.FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}
