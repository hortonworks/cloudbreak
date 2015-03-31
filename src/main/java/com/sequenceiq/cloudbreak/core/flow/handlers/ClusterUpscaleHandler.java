package com.sequenceiq.cloudbreak.core.flow.handlers;

import static com.sequenceiq.cloudbreak.domain.ScalingType.isClusterUpScale;

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
public class ClusterUpscaleHandler extends AbstractFlowHandler<ClusterScalingContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleHandler.class);

    @Override
    protected Object execute(Event<ClusterScalingContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        ClusterScalingContext clusterScalingContext = event.getData();
        if (isClusterUpScale(clusterScalingContext.getScalingType())) {
            FlowContext context = getFlowFacade().upscaleCluster(clusterScalingContext);
            LOGGER.info("Upscale of cluster is finished. Context: {}", context);
            return context;
        }
        return clusterScalingContext;
    }

    @Override
    protected Object handleErrorFlow(Throwable throwable, ClusterScalingContext data) throws Exception {
        LOGGER.info("handleErrorFlow() for phase: {}", getClass());
        data.setErrorReason(throwable.getMessage());
        return getFlowFacade().handleClusterScalingFailure(data);
    }
}
