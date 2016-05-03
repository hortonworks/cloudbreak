package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_ADD_CONTAINERS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_INSTALL_AMBARI_NODES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_SSSD_CONFIG_FINISHED_EVENT;

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
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterUpscaleService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Configuration
public class ClusterUpscaleActions {

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Bean(name = "ADD_CLUSTER_CONTAINERS_STATE")
    public Action addClusterContainersAction() {
        return new AbstractContextCreator() {

            @Override
            protected void doExecute(ClusterUpscaleContext context, ClusterScalingContext payload, Map<Object, Object> variables) throws Exception {
                clusterUpscaleService.addClusterContainers(context, payload);
                sendEvent(context.getFlowId(), CLUSTER_UPSCALE_ADD_CONTAINERS_FINISHED_EVENT.stringRepresentation(), payload);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return null;
            }
        };
    }

    @Bean(name = "INSTALL_AMBARI_NODES_STATE")
    public Action installAmbariNodesAction() {
        return new AbstractContextCreator() {

            @Inject
            private AmbariClusterConnector ambariClusterConnector;

            @Override
            protected void doExecute(final ClusterUpscaleContext context, final ClusterScalingContext payload, Map<Object, Object> variables)
                    throws Exception {
                Set<HostMetadata> hostMetadata = clusterUpscaleService.upscaleCluster(context, payload);
                ambariClusterConnector.reckForHosts(context.getStack(), hostMetadata, new PollingService.Callback() {
                    @Override
                    public void call(PollingResult result) {
                        if (result.equals(PollingResult.SUCCESS)) {
                            sendEvent(context.getFlowId(), CLUSTER_UPSCALE_INSTALL_AMBARI_NODES_FINISHED_EVENT.stringRepresentation(), payload);
                        } else {
                            Object failPayload = getFailurePayload(context, "Installing Ambari nodes failed. Error type: " + result);
                            sendEvent(context.getFlowId(), CLUSTER_UPSCALE_FAILURE_EVENT.stringRepresentation(), failPayload);
                        }
                    }
                });
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return null;
            }
        };
    }

    @Bean(name = "CONFIGURE_SSSD_STATE")
    public Action configureSssdAction() {
        return new AbstractContextCreator() {

            @Override
            protected void doExecute(final ClusterUpscaleContext context, final ClusterScalingContext payload, Map<Object, Object> variables)
                    throws Exception {
                clusterUpscaleService.configureSssd(context, new PollingService.Callback() {
                    @Override
                    public void call(PollingResult result) {
                        if (result.equals(PollingResult.SUCCESS)) {
                            sendEvent(context.getFlowId(), CLUSTER_UPSCALE_SSSD_CONFIG_FINISHED_EVENT.stringRepresentation(), payload);
                        } else {
                            Object failPayload = getFailurePayload(context, "Configuring SSSD failed. Error type: " + result);
                            sendEvent(context.getFlowId(), CLUSTER_UPSCALE_FAILURE_EVENT.stringRepresentation(), failPayload);
                        }
                    }
                });
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return null;
            }
        };
    }

    private abstract class AbstractContextCreator extends AbstractClusterUpscaleAction<ClusterUpscaleContext> {

        @Inject
        private StackService stackService;

        @Override
        protected ClusterUpscaleContext createFlowContext(StateContext<ClusterUpscaleState, ClusterUpscaleEvent> stateContext,
                ClusterScalingContext payload) {
            String flowId = (String) stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_ID.name());
            Stack stack = stackService.getById(payload.getStackId());
            MDCBuilder.buildMdcContext(stack);
            return new ClusterUpscaleContext(flowId, stack);
        }
    }
}