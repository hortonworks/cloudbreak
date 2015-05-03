package com.sequenceiq.cloudbreak.core.flow;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.core.flow.context.BootstrapApiContext;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class BootstrapApiCheckerTask extends StackBasedStatusCheckerTask<BootstrapApiContext> {

    @Override
    public boolean checkStatus(BootstrapApiContext bootstrapApiContext) {
        return bootstrapApiContext.getContainerOrchestrator().isBootstrapApiAvailable(bootstrapApiContext.getGatewayAddress());
    }

    @Override
    public void handleTimeout(BootstrapApiContext t) {
        throw new InternalServerException("Operation timed out. Could not reach bootstrap API in time.");
    }

    @Override
    public String successMessage(BootstrapApiContext t) {
        return "Bootstrap API is available.";
    }
}
