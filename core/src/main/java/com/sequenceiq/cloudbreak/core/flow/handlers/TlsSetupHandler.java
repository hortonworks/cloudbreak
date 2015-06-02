package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;

import reactor.bus.Event;

@Component
public class TlsSetupHandler extends AbstractFlowHandler<ProvisioningContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSetupHandler.class);

    @Override
    protected Object execute(Event<ProvisioningContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        ProvisioningContext provisioningContext = (ProvisioningContext) getFlowFacade().setupTls(event.getData());
        LOGGER.info("TLS setup is done. Context: {}", provisioningContext);
        return provisioningContext;
    }
}


