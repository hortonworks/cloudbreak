package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;

import reactor.event.Event;

@Component
public class ClusterCreationHandler extends AbstractFlowHandler<ProvisioningContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationHandler.class);

    @Override
    protected Object execute(Event<ProvisioningContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        ProvisioningContext provisioningContext = (ProvisioningContext) getFlowFacade().buildAmbariCluster(event.getData());
        LOGGER.info("Cluster created. Context: {}", provisioningContext);
        return provisioningContext;
    }

    @Override
    protected void handleErrorFlow(Throwable throwable, Object data) {
        LOGGER.info("handleErrorFlow() for phase: {}", ((Event) data).getKey());
        try {
            FlowContext context = getFlowFacade().clusterCreationFailed((FlowContext) data);
        } catch (CloudbreakException e) {
            LOGGER.error("Error during handling cluster creation failure. Ex.:", e);
        }
    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        LOGGER.info("assemblePayload() for phase: {}", serviceResult);
        return serviceResult;
    }
}
