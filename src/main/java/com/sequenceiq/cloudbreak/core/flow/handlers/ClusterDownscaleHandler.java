package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;

import reactor.event.Event;

@Component
public class ClusterDownscaleHandler extends AbstractFlowHandler<ClusterScalingContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDownscaleHandler.class);

    @Override
    protected Object execute(Event<ClusterScalingContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        FlowContext context = getFlowFacade().downscaleCluster(event.getData());
        LOGGER.info("Downscale of cluster is finished. Context: {}", context);
        return context;
    }

    @Override
    protected void handleErrorFlow(Throwable throwable, Object data) {
        Event<ClusterScalingContext> event = (Event<ClusterScalingContext>) data;
        ClusterScalingContext scalingContext = event.getData();
        try {
            getFlowFacade().handleClusterScalingFailure(scalingContext);
        } catch (CloudbreakException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        return serviceResult;
    }
}
