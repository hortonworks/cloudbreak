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
public class ProvisioningSetupHandler extends AbstractFlowHandler<ProvisioningContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningSetupHandler.class);

    @Override
    protected Object execute(Event<ProvisioningContext> event) throws CloudbreakException {
        LOGGER.info("Executing provisioning setup logic. Event: {}", event);
        FlowContext provisioningContext = event.getData();
        provisioningContext = getFlowFacade().setup(provisioningContext);
        return provisioningContext;
    }

    @Override
    protected Object handleErrorFlow(Throwable throwable, ProvisioningContext data) throws Exception {
        LOGGER.debug("Handling error during provisioning setup. Exception {}, Data: {}", throwable.getClass(), data);
        return data;
    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        LOGGER.debug("Assembling payload for the next phase. Data: {}", serviceResult);
        return serviceResult;
    }
}
