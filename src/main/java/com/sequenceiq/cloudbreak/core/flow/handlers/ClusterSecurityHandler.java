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
public class ClusterSecurityHandler extends AbstractFlowHandler<ProvisioningContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSecurityHandler.class);

    @Override
    protected Object execute(Event<ProvisioningContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        ProvisioningContext provisioningContext = (ProvisioningContext) getFlowFacade().enableSecurity(event.getData());
        return provisioningContext;
    }

    @Override
    protected Object handleErrorFlow(Throwable throwable, ProvisioningContext data) throws Exception {
        LOGGER.info("handleErrorFlow() for phase: {}", getClass());
        data.setErrorReason(throwable.getMessage());
        CloudbreakException exception = (CloudbreakException) throwable;
        if (exception.getCause() instanceof InterruptedException) {
            LOGGER.info("Enable kerberos flow has been interrupted");
        } else {
            return getFlowFacade().handleSecurityEnableFailure(data);
        }
        return data;
    }
}
