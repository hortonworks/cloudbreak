package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;

@Configuration
public class ClusterDownscaleActions {
    @Inject
    private ClusterDownscaleService clusterDownscaleService;

    @Bean(name = "COLLECT_CANDIDATES_STATE")
    public Action collectCandidatesAction() {
        return new AbstractClusterAction<ClusterDownscaleTriggerEvent>(ClusterDownscaleTriggerEvent.class) {
            @Override
            protected void doExecute(ClusterContext context, ClusterDownscaleTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                clusterDownscaleService.clusterDownscaleStarted(context.getStack(), payload.getHostGroupName(), payload.getAdjustment(),
                        payload.getHostNames());
                CollectDownscaleCandidatesRequest request = new CollectDownscaleCandidatesRequest(context.getStack().getId(), payload.getHostGroupName(),
                        payload.getAdjustment(), payload.getHostNames());
                sendEvent(context.getFlowId(), request.selector(), request);
            }
        };
    }

    @Bean(name = "DECOMMISSION_STATE")
    public Action decommissionAction() {
        return new AbstractClusterAction<CollectDownscaleCandidatesResult>(CollectDownscaleCandidatesResult.class) {
            @Override
            protected void doExecute(ClusterContext context, CollectDownscaleCandidatesResult payload, Map<Object, Object> variables) throws Exception {
                DecommissionRequest request = new DecommissionRequest(context.getStack().getId(), payload.getHostGroupName(), payload.getHostNames());
                sendEvent(context.getFlowId(), request.selector(), request);
            }
        };
    }

    @Bean(name = "UPDATE_INSTANCE_METADATA_STATE")
    public Action updateInstanceMetadataAction() {
        return new AbstractClusterAction<DecommissionResult>(DecommissionResult.class) {
            @Override
            protected void doExecute(ClusterContext context, DecommissionResult payload, Map<Object, Object> variables) throws Exception {
                clusterDownscaleService.updateMetadata(context.getStack().getId(), payload.getHostNames(), payload.getRequest().getHostGroupName());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new StackEvent(ClusterDownscaleEvent.FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_DOWNSCALE_FAILED_STATE")
    public Action clusterDownscalescaleFailedAction() {
        return new AbstractStackFailureAction<ClusterStartState, ClusterStartEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                clusterDownscaleService.handleClusterDownscaleFailure(context.getStack(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterDownscaleEvent.FAIL_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }
}
