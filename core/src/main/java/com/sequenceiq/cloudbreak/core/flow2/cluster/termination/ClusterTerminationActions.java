package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.DisableKerberosResultToStackFailureEventConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.DisableKerberosResultToTerminationEventConverter;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DeregisterServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DeregisterServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DisableKerberosRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.PrepareClusterTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.PrepareClusterTerminationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.cloudbreak.structuredevent.job.StructuredSynchronizerJobService;
import com.sequenceiq.flow.core.PayloadConverter;

@Configuration
public class ClusterTerminationActions {
    @Inject
    private ClusterTerminationFlowService clusterTerminationFlowService;

    @Inject
    private StatusCheckerJobService jobService;

    @Inject
    private StructuredSynchronizerJobService syncJobService;

    @Bean(name = "PREPARE_CLUSTER_STATE")
    public Action<?, ?> prepareCluster() {
        return new AbstractClusterTerminationAction<>(TerminationEvent.class) {

            @Override
            protected void doExecute(ClusterTerminationContext context, TerminationEvent payload, Map<Object, Object> variables) {
                clusterTerminationFlowService.terminateCluster(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterTerminationContext context) {
                return new PrepareClusterTerminationRequest(context.getStackId(), context.isForced());
            }

            @Override
            protected void prepareExecution(TerminationEvent payload, Map<Object, Object> variables) {
                variables.put(TERMINATION_TYPE, payload.getTerminationType());
            }

        };
    }

    @Bean(name = "DEREGISTER_SERVICES_STATE")
    public Action<?, ?> deregisterServices() {
        return new AbstractClusterTerminationAction<>(PrepareClusterTerminationResult.class) {
            @Override
            protected void doExecute(ClusterTerminationContext context, PrepareClusterTerminationResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterTerminationContext context) {
                return new DeregisterServicesRequest(context.getStackId(), context.isForced());
            }
        };
    }

    @Bean(name = "DISABLE_KERBEROS_STATE")
    public Action<?, ?> disableKerberos() {
        return new AbstractClusterTerminationAction<>(DeregisterServicesResult.class) {
            @Override
            protected void doExecute(ClusterTerminationContext context, DeregisterServicesResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterTerminationContext context) {
                return new DisableKerberosRequest(context.getStackId(), context.isForced());
            }
        };
    }

    @Bean(name = "CLUSTER_TERMINATING_STATE")
    public Action<?, ?> terminatingCluster() {
        return new AbstractClusterTerminationAction<>(TerminationEvent.class) {
            @Override
            protected void doExecute(ClusterTerminationContext context, TerminationEvent payload, Map<Object, Object> variables) {
                clusterTerminationFlowService.terminateCluster(context);
                sendEvent(context);
            }

            @Override
            protected void prepareExecution(TerminationEvent payload, Map<Object, Object> variables) {
                variables.put(TERMINATION_TYPE, payload.getTerminationType());
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<TerminationEvent>> payloadConverters) {
                payloadConverters.add(new DisableKerberosResultToTerminationEventConverter());
            }

            @Override
            protected Selectable createRequest(ClusterTerminationContext context) {
                return new ClusterTerminationRequest(context.getStackId(),
                        context.getCluster() != null ? context.getCluster().getId() : null,
                        context.isForced());
            }
        };
    }

    @Bean(name = "CLUSTER_TERMINATION_FINISH_STATE")
    public Action<?, ?> clusterTerminationFinished() {
        return new AbstractClusterTerminationAction<>(ClusterTerminationResult.class) {
            @Override
            protected void doExecute(ClusterTerminationContext context, ClusterTerminationResult payload, Map<Object, Object> variables) {
                clusterTerminationFlowService.finishClusterTerminationAllowed(context, payload);
                jobService.unschedule(String.valueOf(context.getStackId()));
                syncJobService.unschedule(String.valueOf(context.getStackId()));
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterTerminationContext context) {
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
            protected void initPayloadConverterMap(List<PayloadConverter<StackFailureEvent>> payloadConverters) {
                super.initPayloadConverterMap(payloadConverters);
                payloadConverters.add(new DisableKerberosResultToStackFailureEventConverter());
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterTerminationEvent.FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }
}
