package com.sequenceiq.cloudbreak.core.bootstrap.service.container;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.bootstrap.service.container.context.ContainerBootstrapApiContext;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class ContainerBootstrapApiCheckerTask extends StackBasedStatusCheckerTask<ContainerBootstrapApiContext> {

    @Override
    public boolean checkStatus(ContainerBootstrapApiContext containerBootstrapApiContext) {
        return containerBootstrapApiContext.getContainerOrchestrator().isBootstrapApiAvailable(containerBootstrapApiContext.getGatewayConfig());
    }

    @Override
    public void handleTimeout(ContainerBootstrapApiContext t) {
        throw new CloudbreakServiceException("Operation timed out. Could not reach bootstrap API in time.");
    }

    @Override
    public String successMessage(ContainerBootstrapApiContext t) {
        return "Bootstrap API is available.";
    }
}
