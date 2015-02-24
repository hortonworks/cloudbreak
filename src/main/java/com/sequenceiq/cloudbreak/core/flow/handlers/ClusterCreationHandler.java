package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.ProvisioningFacade;

import reactor.event.Event;

@Component
public class ClusterCreationHandler extends AbstractFlowHandler<ProvisioningContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationHandler.class);

    @Autowired
    private ProvisioningFacade provisioningFacade;

    @Override
    protected Object execute(Event<ProvisioningContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        ProvisioningContext provisioningContext = provisioningFacade.buildAmbariCluster(event.getData());
        LOGGER.info("Cluster created. Context: {}", provisioningContext);
        return provisioningContext;
    }

    @Override
    protected void handleErrorFlow(Throwable throwable, Object data) {
        LOGGER.info("handleErrorFlow() for phase: {}", ((Event) data).getKey());
    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        LOGGER.info("assemblePayload() for phase: {}", ((Event) serviceResult).getKey());
        return serviceResult;
    }
}
