package com.sequenceiq.cloudbreak.core.flow.handlers;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.ErrorHandlerAwareFlowEventFactory;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ExtendConsulMetadataHandler extends AbstractFlowHandler<ClusterScalingContext> implements FlowHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendConsulMetadataHandler.class);

    @Inject
    private EventBus reactor;

    @Inject
    private ErrorHandlerAwareFlowEventFactory eventFactory;

    @Override
    protected Object execute(Event<ClusterScalingContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        ClusterScalingContext context = (ClusterScalingContext) getFlowFacade().extendConsulMetadata(event.getData());
        LOGGER.info("Extending consul metadata is finished. Context: {}", context);
        if (ScalingType.isClusterUpScale(context.getScalingType())) {
            reactor.notify(FlowPhases.ADD_CLUSTER_CONTAINERS.name(), eventFactory.createEvent(context, FlowPhases.ADD_CLUSTER_CONTAINERS.name()));
        }
        return context;
    }

    @Override
    protected Object handleErrorFlow(Throwable throwable, ClusterScalingContext data) throws Exception {
        LOGGER.info("handleErrorFlow() for phase: {}", getClass());
        data.setErrorReason(throwable.getMessage());
        return getFlowFacade().handleStackScalingFailure(data);
    }
}
