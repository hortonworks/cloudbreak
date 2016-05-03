package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_FAIL_HANDLED_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterUpscaleService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component("ClusterUpscaleFailAction")
public class ClusterUpscaleFailAction extends AbstractAction<ClusterUpscaleState, ClusterUpscaleEvent, ClusterUpscaleContext,
        UpscaleClusterFailedPayload> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleFailAction.class);

    @Inject
    private StackService stackService;
    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    protected ClusterUpscaleFailAction() {
        super(UpscaleClusterFailedPayload.class);
    }

    @Override
    protected ClusterUpscaleContext createFlowContext(StateContext<ClusterUpscaleState, ClusterUpscaleEvent> stateContext,
            UpscaleClusterFailedPayload payload) {
        String flowId = (String) stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_ID.name());
        Stack stack = stackService.getById(payload.getStackId());
        MDCBuilder.buildMdcContext(stack);
        return new ClusterUpscaleContext(flowId, stack);
    }

    @Override
    protected void doExecute(ClusterUpscaleContext context, UpscaleClusterFailedPayload payload, Map<Object, Object> variables) throws Exception {
        LOGGER.error("Error during Cluster upscale flow: " + payload.getErrorReason());
        clusterUpscaleService.handleFailure(context, payload);
        sendEvent(context.getFlowId(), CLUSTER_UPSCALE_FAIL_HANDLED_EVENT.stringRepresentation(), payload);
    }

    @Override
    protected Selectable createRequest(ClusterUpscaleContext context) {
        return null;
    }

    @Override
    protected Object getFailurePayload(ClusterUpscaleContext flowContext, Exception ex) {
        return null;
    }
}
