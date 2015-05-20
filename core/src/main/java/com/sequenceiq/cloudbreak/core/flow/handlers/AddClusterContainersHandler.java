package com.sequenceiq.cloudbreak.core.flow.handlers;

import static com.sequenceiq.cloudbreak.domain.ScalingType.isClusterUpScale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;

import reactor.event.Event;

@Component
public class AddClusterContainersHandler extends AbstractFlowHandler<ClusterScalingContext> implements FlowHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddClusterContainersHandler.class);

    @Override
    protected Object execute(Event<ClusterScalingContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        ClusterScalingContext clusterScalingContext = event.getData();
        if (isClusterUpScale(clusterScalingContext.getScalingType())) {
            ClusterScalingContext context = (ClusterScalingContext) getFlowFacade().addClusterContainers(event.getData());
            LOGGER.info("Added cluster containers. Context: {}", context);
            return context;
        }
        return clusterScalingContext;
    }
}
