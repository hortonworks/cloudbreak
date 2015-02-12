package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.ProvisioningFacade;

import reactor.event.Event;

@Component
public class ProvisioningSetupHandler extends AbstractFlowHandler<ProvisioningContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningSetupHandler.class);

    @Autowired
    private ProvisioningFacade provisioningFacade;

    @Override
    protected Object execute(Event<ProvisioningContext> event) throws CloudbreakException {
        LOGGER.info("Executing provisioning setup logic. Event: {}", event);
        ProvisioningContext provisioningContext = event.getData();
        provisioningFacade.setup(provisioningContext);
        return provisioningContext;
    }

    @Override
    protected void handleErrorFlow(Throwable throwable, Object data) {
        LOGGER.debug("Handling error during provisioning setup. Exception {}, Data: {}", throwable.getClass(), data);
    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        LOGGER.debug("Assembling payload for the next phase. Data: {}", serviceResult);
        return serviceResult;
    }
}
