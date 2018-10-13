package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FINALIZED_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.MetricType;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterResult;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleAmbariRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleAmbariResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadUpscaleRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadUpscaleRecipesResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Configuration
public class ClusterUpscaleActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleActions.class);

    @Inject
    private ClusterUpscaleFlowService clusterUpscaleFlowService;

    @Bean(name = "UPLOAD_UPSCALE_RECIPES_STATE")
    public Action<?, ?> uploadUpscaleRecipesAction() {
        return new AbstractClusterUpscaleAction<ClusterScaleTriggerEvent>(ClusterScaleTriggerEvent.class) {
            @Override
            protected void prepareExecution(ClusterScaleTriggerEvent payload, Map<Object, Object> variables) {
                variables.put(HOSTGROUPNAME, payload.getHostGroupName());
                variables.put(ADJUSTMENT, payload.getAdjustment());
            }

            @Override
            protected void doExecute(ClusterUpscaleContext context, ClusterScaleTriggerEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new UploadUpscaleRecipesRequest(context.getStackId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "UPSCALING_AMBARI_STATE")
    public Action<?, ?> upscalingAmbariAction() {
        return new AbstractClusterUpscaleAction<UploadUpscaleRecipesResult>(UploadUpscaleRecipesResult.class) {
            @Override
            protected void doExecute(ClusterUpscaleContext context, UploadUpscaleRecipesResult payload, Map<Object, Object> variables) {
                clusterUpscaleFlowService.upscalingAmbari(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new UpscaleAmbariRequest(context.getStackId(), context.getHostGroupName(), context.getAdjustment());
            }

        };
    }

    @Bean(name = "UPSCALING_CLUSTER_STATE")
    public Action<?, ?> installServicesAction() {
        return new AbstractClusterUpscaleAction<UpscaleAmbariResult>(UpscaleAmbariResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, UpscaleAmbariResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new UpscaleClusterRequest(context.getStackId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "EXECUTING_POSTRECIPES_STATE")
    public Action<?, ?> executePostRecipesAction() {
        return new AbstractClusterUpscaleAction<UpscaleClusterResult>(UpscaleClusterResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, UpscaleClusterResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new UpscalePostRecipesRequest(context.getStackId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "FINALIZE_UPSCALE_STATE")
    public Action<?, ?> upscaleFinishedAction() {
        return new AbstractClusterUpscaleAction<UpscalePostRecipesResult>(UpscalePostRecipesResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, UpscalePostRecipesResult payload, Map<Object, Object> variables) {
                clusterUpscaleFlowService.clusterUpscaleFinished(context.getStack(), payload.getHostGroupName());
                metricService.incrementMetricCounter(MetricType.CLUSTER_UPSCALE_SUCCESSFUL, context.getStack());
                sendEvent(context.getFlowId(), FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return null;
            }
        };
    }

    @Bean(name = "CLUSTER_UPSCALE_FAILED_STATE")
    public Action<?, ?> clusterUpscaleFailedAction() {
        return new AbstractStackFailureAction<ClusterUpscaleState, ClusterUpscaleEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                clusterUpscaleFlowService.clusterUpscaleFailed(context.getStackView().getId(), payload.getException());
                metricService.incrementMetricCounter(MetricType.CLUSTER_UPSCALE_FAILED, context.getStackView());
                sendEvent(context.getFlowId(), FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }

    private abstract static class AbstractClusterUpscaleAction<P extends Payload>
        extends AbstractAction<ClusterUpscaleState, ClusterUpscaleEvent, ClusterUpscaleContext, P> {
        protected static final String HOSTGROUPNAME = "HOSTGROUPNAME";

        protected static final String ADJUSTMENT = "ADJUSTMENT";

        @Inject
        private StackService stackService;

        protected AbstractClusterUpscaleAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<ClusterUpscaleContext> flowContext, Exception ex) {
            return new StackFailureEvent(payload.getStackId(), ex);
        }

        @Override
        protected ClusterUpscaleContext createFlowContext(String flowId, StateContext<ClusterUpscaleState, ClusterUpscaleEvent> stateContext, P payload) {
            Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
            StackView stack = stackService.getViewByIdWithoutAuth(payload.getStackId());
            MDCBuilder.buildMdcContext(stack.getId().toString(), stack.getName(), "CLUSTER");
            return new ClusterUpscaleContext(flowId, stack, getHostgroupName(variables), getAdjustment(variables));
        }

        private String getHostgroupName(Map<Object, Object> variables) {
            return (String) variables.get(HOSTGROUPNAME);
        }

        private Integer getAdjustment(Map<Object, Object> variables) {
            return (Integer) variables.get(ADJUSTMENT);
        }
    }
}
