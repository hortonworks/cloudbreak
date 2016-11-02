package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.FINALIZED_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterScaleContext;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.ClusterScalePayload;
import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.ScalingAdjustmentPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateInstanceMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateInstanceMetadataResult;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Configuration
public class ClusterDownscaleActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDownscaleActions.class);

    private static final String SCALING_ADJUSTMENT = "SCALING_ADJUSTMENT";

    @Inject
    private StackService stackService;

    @Inject
    private FlowMessageService flowMessageService;

    @Bean(name = "DECOMMISSION_STATE")
    public Action decommissionAction() {
        return new AbstractClusterDownscaleAction<ClusterDecommissionContext, ClusterScaleTriggerEvent>(ClusterScaleTriggerEvent.class) {

            @Override
            protected ClusterDecommissionContext createFlowContext(String flowId, StateContext<ClusterDownscaleState, ClusterDownscaleEvent> stateContext,
                    ClusterScaleTriggerEvent payload) {
                Stack stack = stackService.getById(payload.getStackId());
                MDCBuilder.buildMdcContext(stack);
                return new ClusterDecommissionContext(flowId, stack, payload.getHostGroupName(), payload.getAdjustment());
            }

            @Override
            protected void doExecute(ClusterDecommissionContext context, ClusterScaleTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                variables.put(SCALING_ADJUSTMENT, context.getScalingAdjustment());
                flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.AMBARI_CLUSTER_SCALING_DOWN, UPDATE_IN_PROGRESS.name());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterDecommissionContext context) {
                return new DecommissionRequest(context.getStack().getId(), context.getHostGroupName(), context.getScalingAdjustment());
            }
        };
    }

    @Bean(name = "UPDATE_INSTANCE_METADATA_STATE")
    public Action updateInstanceMetadataAction() {
        return new AbstractClusterDownscaleAction<ClusterUpdateMetadataContext, DecommissionResult>(DecommissionResult.class) {

            @Override
            protected ClusterUpdateMetadataContext createFlowContext(String flowId, StateContext<ClusterDownscaleState, ClusterDownscaleEvent> stateContext,
                    DecommissionResult payload) {
                Stack stack = stackService.getById(payload.getStackId());
                MDCBuilder.buildMdcContext(stack);
                return new ClusterUpdateMetadataContext(flowId, stack, payload.getRequest().getHostGroupName(), payload.getHostNames());
            }

            @Override
            protected void doExecute(ClusterUpdateMetadataContext context, DecommissionResult payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpdateMetadataContext context) {
                return new UpdateInstanceMetadataRequest(context.getStack().getId(), context.getHostGroupName(), context.getHostNames());
            }
        };
    }

    @Bean(name = "FINALIZE_DOWNSCALE_STATE")
    public Action finalizeDownscaleAction() {
        return new AbstractClusterDownscaleAction<ClusterScaleContext, UpdateInstanceMetadataResult>(UpdateInstanceMetadataResult.class) {

            @Override
            protected void doExecute(ClusterScaleContext context, UpdateInstanceMetadataResult payload, Map<Object, Object> variables) throws Exception {
                flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.AMBARI_CLUSTER_SCALED_DOWN, AVAILABLE.name());
                ScalingAdjustmentPayload nextPayload =
                        new ClusterScalePayload(context.getStack().getId(), context.getHostGroupName(), (Integer) variables.get(SCALING_ADJUSTMENT));
                sendEvent(context.getFlowId(), FINALIZED_EVENT.stringRepresentation(), nextPayload);
            }

            @Override
            protected Selectable createRequest(ClusterScaleContext context) {
                return null;
            }
        };
    }

    @Bean(name = "CLUSTER_DOWNSCALE_FAILED_STATE")
    public Action clusterDownscalescaleFailedAction() {
        return new AbstractClusterDownscaleAction<ClusterScaleContext, DownscaleClusterFailurePayload>(DownscaleClusterFailurePayload.class) {
            @Inject
            private StackUpdater stackUpdater;
            @Inject
            private ClusterService clusterService;
            @Inject
            private FlowMessageService flowMessageService;

            @Override
            protected void doExecute(ClusterScaleContext context, DownscaleClusterFailurePayload payload, Map<Object, Object> variables) throws Exception {
                getFlow(context.getFlowId()).setFlowFailed();
                Stack stack = context.getStack();
                String message = payload.getErrorDetails().getMessage();
                LOGGER.error("Error during Cluster downscale flow: " + message, payload.getErrorDetails());
                clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_FAILED, message);
                stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Node(s) could not be removed from the cluster: " + message);
                flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "removed from", message);
                sendEvent(context.getFlowId(), FAIL_HANDLED_EVENT.stringRepresentation(), payload);
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<DownscaleClusterFailurePayload>> payloadConverters) {
                payloadConverters.add(new PayloadConverter<DownscaleClusterFailurePayload>() {
                    @Override
                    public boolean canConvert(Class sourceClass) {
                        return AbstractClusterScaleResult.class.isAssignableFrom(sourceClass);
                    }

                    @Override
                    public DownscaleClusterFailurePayload convert(Object payload) {
                        AbstractClusterScaleResult result = (AbstractClusterScaleResult) payload;
                        return new DownscaleClusterFailurePayload(result.getStackId(), result.getHostGroupName(), result.getErrorDetails());
                    }
                });
            }

            @Override
            protected Selectable createRequest(ClusterScaleContext context) {
                return null;
            }
        };
    }

    private abstract class AbstractClusterDownscaleAction<C extends ClusterScaleContext, P extends HostGroupPayload>
            extends AbstractAction<ClusterDownscaleState, ClusterDownscaleEvent, C, P> {

        AbstractClusterDownscaleAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected C createFlowContext(String flowId, StateContext<ClusterDownscaleState, ClusterDownscaleEvent> stateContext, P payload) {
            Stack stack = stackService.getById(payload.getStackId());
            MDCBuilder.buildMdcContext(stack);
            return (C) new ClusterScaleContext(flowId, stack, payload.getHostGroupName());
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<C> flowContext, Exception ex) {
            return new DownscaleClusterFailurePayload(payload.getStackId(), payload.getHostGroupName(), ex);
        }
    }
}
