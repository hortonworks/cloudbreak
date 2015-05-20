package com.sequenceiq.cloudbreak.core.bootstrap.service;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow.context.BootstrapApiContext;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class BootstrapApiCheckerTask extends StackBasedStatusCheckerTask<BootstrapApiContext> {

    @Override
    public boolean checkStatus(BootstrapApiContext bootstrapApiContext) {
        return bootstrapApiContext.getContainerOrchestrator().isBootstrapApiAvailable(bootstrapApiContext.getGatewayAddress());
    }

    @Override
    public void handleTimeout(BootstrapApiContext t) {
        throw new CloudbreakServiceException("Operation timed out. Could not reach bootstrap API in time.");
    }

    @Override
    public String successMessage(BootstrapApiContext t) {
        return "Bootstrap API is available.";
    }
}
