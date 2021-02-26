package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostBootstrapApiContext;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class HostBootstrapApiCheckerTask extends StackBasedStatusCheckerTask<HostBootstrapApiContext> {

    @Override
    public boolean checkStatus(HostBootstrapApiContext hostBootstrapApiContext) {
        return hostBootstrapApiContext.getHostOrchestrator().isBootstrapApiAvailable(hostBootstrapApiContext.getGatewayConfig());
    }

    @Override
    public void handleTimeout(HostBootstrapApiContext t) {
        throw new CloudbreakServiceException("Operation timed out. Could not reach bootstrap API in time. "
                + "The Control Plane was not able to establish the connection with the gateway instance. "
                + "This could be caused by the reverse SSH tunnel (autossh process) running on this instance could not connect to the Cloudera server. "
                + "Please check your connection and proxy settings and make sure the instance can reach *.ccm.cdp.cloudera.com "
                + "Please check your instance on the cloud provider side if it's up and running. Restart it if it couldn't start up properly.");
    }

    @Override
    public String successMessage(HostBootstrapApiContext t) {
        return "Bootstrap API is available on gateway: " + t.getGatewayConfig().getPrivateAddress();
    }
}
