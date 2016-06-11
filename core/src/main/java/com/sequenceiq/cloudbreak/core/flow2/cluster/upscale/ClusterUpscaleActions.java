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
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterResult;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleAmbariRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleAmbariResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePreRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePreRecipesResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Configuration
public class ClusterUpscaleActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleActions.class);
    @Inject
    private ClusterUpscaleFlowService clusterUpscaleFlowService;

    @Bean(name = "UPSCALING_AMBARI_STATE")
    public Action upscalingAmbariAction() {
        return new AbstractClusterUpscaleAction<ClusterScaleTriggerEvent>(ClusterScaleTriggerEvent.class) {
            @Override
            protected void prepareExecution(ClusterScaleTriggerEvent payload, Map<Object, Object> variables) {
                variables.put(HOSTGROUPNAME, payload.getHostGroupName());
                variables.put(ADJUSTMENT, payload.getAdjustment());
            }

            @Override
            protected void doExecute(final ClusterUpscaleContext context, final ClusterScaleTriggerEvent payload, Map<Object, Object> variables)
                    throws Exception {
                clusterUpscaleFlowService.upscalingAmbari(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new UpscaleAmbariRequest(context.getStack().getId(), context.getHostGroupName(), context.getAdjustment());
            }

        };
    }

    @Bean(name = "EXECUTING_PRERECIPES_STATE")
    public Action executingPrerecipesAction() {
        return new AbstractClusterUpscaleAction<UpscaleAmbariResult>(UpscaleAmbariResult.class) {
            @Override
            protected void doExecute(final ClusterUpscaleContext context, final UpscaleAmbariResult payload, Map<Object, Object> variables)
                    throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new UpscalePreRecipesRequest(context.getStack().getId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "UPSCALING_CLUSTER_STATE")
    public Action installServicesAction() {
        return new AbstractClusterUpscaleAction<UpscalePreRecipesResult>(UpscalePreRecipesResult.class) {

            @Override
            protected void doExecute(final ClusterUpscaleContext context, final UpscalePreRecipesResult payload, Map<Object, Object> variables)
                    throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new UpscaleClusterRequest(context.getStack().getId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "EXECUTING_POSTRECIPES_STATE")
    public Action executePostRecipesAction() {
        return new AbstractClusterUpscaleAction<UpscaleClusterResult>(UpscaleClusterResult.class) {

            @Override
            protected void doExecute(final ClusterUpscaleContext context, final UpscaleClusterResult payload, Map<Object, Object> variables)
                    throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new UpscalePostRecipesRequest(context.getStack().getId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "FINALIZE_UPSCALE_STATE")
    public Action upscaleFinishedAction() {
        return new AbstractClusterUpscaleAction<UpscalePostRecipesResult>(UpscalePostRecipesResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, UpscalePostRecipesResult payload, Map<Object, Object> variables) throws Exception {
                clusterUpscaleFlowService.clusterUpscaleFinished(context.getStack(), payload.getHostGroupName());
                sendEvent(context.getFlowId(), FINALIZED_EVENT.stringRepresentation(), payload);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return null;
            }
        };
    }

    @Bean(name = "CLUSTER_UPSCALE_FAILED_STATE")
    public Action clusterUpscaleFailedAction() {
        return new AbstractStackFailureAction<ClusterUpscaleState, ClusterUpscaleEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                clusterUpscaleFlowService.clusterUpscaleFailed(context.getStack(), payload.getException());
                sendEvent(context.getFlowId(), FAIL_HANDLED_EVENT.stringRepresentation(), payload);
            }
        };
    }

    private abstract class AbstractClusterUpscaleAction<P extends Payload>
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
            Stack stack = stackService.getById(payload.getStackId());
            MDCBuilder.buildMdcContext(stack.getCluster());
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
