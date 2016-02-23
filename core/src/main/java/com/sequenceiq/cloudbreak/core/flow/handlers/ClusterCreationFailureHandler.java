package com.sequenceiq.cloudbreak.core.flow.handlers;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.bus.Event;

@Service
public class ClusterCreationFailureHandler extends AbstractFlowHandler<ProvisioningContext> implements FlowHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariStartHandler.class);

    @Override
    protected Object execute(Event<ProvisioningContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        ProvisioningContext provisioningContext = (ProvisioningContext) getFlowFacade().handleClusterCreationFailure(event.getData());
        LOGGER.info("Ambari started. Context: {}", provisioningContext);
        return provisioningContext;
    }
}
