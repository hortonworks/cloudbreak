package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;

import reactor.event.Event;

@Component
public class ClusterSetupHandler extends AbstractFlowHandler<ProvisioningContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSetupHandler.class);

    @Override
    protected Object execute(Event<ProvisioningContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        ProvisioningContext provisioningContext = (ProvisioningContext) getFlowFacade().setupCluster(event.getData());
        LOGGER.info("Munchausen set up. Context: {}", provisioningContext);
        return provisioningContext;
    }
}
