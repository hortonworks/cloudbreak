package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostBootstrapApiContext;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;
import org.springframework.stereotype.Component;

@Component
public class HostBootstrapApiCheckerTask extends StackBasedStatusCheckerTask<HostBootstrapApiContext> {

    @Override
    public boolean checkStatus(HostBootstrapApiContext hostBootstrapApiContext) {
        return hostBootstrapApiContext.getHostOrchestrator().isBootstrapApiAvailable(hostBootstrapApiContext.getGatewayConfig());
    }

    @Override
    public void handleTimeout(HostBootstrapApiContext t) {
        throw new CloudbreakServiceException("Operation timed out. Could not reach bootstrap API in time.");
    }

    @Override
    public String successMessage(HostBootstrapApiContext t) {
        return "Bootstrap API is available on gateway: " + t.getGatewayConfig().getPrivateAddress();
    }
}
