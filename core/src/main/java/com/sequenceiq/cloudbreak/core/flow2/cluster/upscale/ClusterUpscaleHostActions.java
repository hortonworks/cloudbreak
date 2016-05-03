package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_EXECUTE_POST_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_EXECUTE_PRE_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_INSTALL_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_INSTALl_SERVICES_FINISHED_EVENT;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterUpscaleService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Configuration
public class ClusterUpscaleHostActions {

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Bean(name = "INSTALL_RECIPES_STATE")
    public Action installRecipesAction() {
        return new AbstractContextCreator() {

            @Override
            protected void doExecute(final ClusterUpscaleHostContext context, final ClusterScalingContext payload, Map<Object, Object> variables)
                    throws Exception {
                clusterUpscaleService.installRecipes(context, new PollingService.Callback() {
                    @Override
                    public void call(PollingResult result) {
                        if (result.equals(PollingResult.SUCCESS)) {
                            sendEvent(context.getFlowId(), CLUSTER_UPSCALE_INSTALL_RECIPES_FINISHED_EVENT.stringRepresentation(), payload);
                        } else {
                            Object failPayload = getFailurePayload(context, "Installing recipes failed. Error type: " + result);
                            sendEvent(context.getFlowId(), CLUSTER_UPSCALE_FAILURE_EVENT.stringRepresentation(), failPayload);
                        }
                    }
                });
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleHostContext context) {
                return null;
            }
        };
    }

    @Bean(name = "EXECUTE_PRE_RECIPES_STATE")
    public Action executePreRecipesAction() {
        return new AbstractContextCreator() {

            @Override
            protected void doExecute(final ClusterUpscaleHostContext context, final ClusterScalingContext payload, Map<Object, Object> variables)
                    throws Exception {
                clusterUpscaleService.executePreRecipes(context, new PollingService.Callback() {
                    @Override
                    public void call(PollingResult result) {
                        if (result.equals(PollingResult.SUCCESS)) {
                            sendEvent(context.getFlowId(), CLUSTER_UPSCALE_EXECUTE_PRE_RECIPES_FINISHED_EVENT.stringRepresentation(), payload);
                        } else {
                            Object failPayload = getFailurePayload(context, "Installing pre recipes failed. Error type: " + result);
                            sendEvent(context.getFlowId(), CLUSTER_UPSCALE_FAILURE_EVENT.stringRepresentation(), failPayload);
                        }
                    }
                });
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleHostContext context) {
                return null;
            }
        };
    }

    @Bean(name = "INSTALL_SERVICES_STATE")
    public Action installServicesAction() {
        return new AbstractContextCreator() {

            @Override
            protected void doExecute(final ClusterUpscaleHostContext context, final ClusterScalingContext payload, Map<Object, Object> variables)
                    throws Exception {
                clusterUpscaleService.installServices(context, new PollingService.Callback() {
                    @Override
                    public void call(PollingResult result) {
                        if (result.equals(PollingResult.SUCCESS)) {
                            sendEvent(context.getFlowId(), CLUSTER_UPSCALE_INSTALl_SERVICES_FINISHED_EVENT.stringRepresentation(), payload);
                        } else {
                            Object failPayload = getFailurePayload(context, "Installing services failed. Error type: " + result);
                            sendEvent(context.getFlowId(), CLUSTER_UPSCALE_FAILURE_EVENT.stringRepresentation(), failPayload);
                        }
                    }
                });
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleHostContext context) {
                return null;
            }
        };
    }

    @Bean(name = "EXECUTE_POST_RECIPES_STATE")
    public Action executePostRecipesAction() {
        return new AbstractContextCreator() {

            @Override
            protected void doExecute(final ClusterUpscaleHostContext context, final ClusterScalingContext payload, Map<Object, Object> variables)
                    throws Exception {
                clusterUpscaleService.executePostRecipes(context, new PollingService.Callback() {
                    @Override
                    public void call(PollingResult result) {
                        if (result.equals(PollingResult.SUCCESS)) {
                            sendEvent(context.getFlowId(), CLUSTER_UPSCALE_EXECUTE_POST_RECIPES_FINISHED_EVENT.stringRepresentation(), payload);
                        } else {
                            Object failPayload = getFailurePayload(context, "Installing post recipes failed. Error type: " + result);
                            sendEvent(context.getFlowId(), CLUSTER_UPSCALE_FAILURE_EVENT.stringRepresentation(), failPayload);
                        }
                    }
                });
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleHostContext context) {
                return null;
            }
        };
    }

    @Bean(name = "FINALIZE_STATE")
    public Action upscaleFinishedAction() {
        return new AbstractContextCreator() {

            @Override
            protected void doExecute(ClusterUpscaleHostContext context, ClusterScalingContext payload, Map<Object, Object> variables) throws Exception {
                clusterUpscaleService.finalizeUpscale(context, payload);
                sendEvent(context.getFlowId(), CLUSTER_UPSCALE_FINALIZED_EVENT.stringRepresentation(), payload);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleHostContext context) {
                return null;
            }
        };
    }

    private abstract class AbstractContextCreator extends AbstractClusterUpscaleAction<ClusterUpscaleHostContext> {

        @Inject
        private StackService stackService;
        @Inject
        private HostGroupService hostGroupService;

        @Override
        protected ClusterUpscaleHostContext createFlowContext(StateContext<ClusterUpscaleState, ClusterUpscaleEvent> stateContext,
                ClusterScalingContext payload) {
            String flowId = (String) stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_ID.name());
            Stack stack = stackService.getById(payload.getStackId());
            MDCBuilder.buildMdcContext(stack);
            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), payload.getHostGroupAdjustment().getHostGroup());
            Set<HostMetadata> hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.getId());
            return new ClusterUpscaleHostContext(flowId, stack, hostGroup, hostMetadata);
        }
    }
}
