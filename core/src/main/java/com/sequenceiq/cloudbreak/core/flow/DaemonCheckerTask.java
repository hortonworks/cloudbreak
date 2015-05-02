package com.sequenceiq.cloudbreak.core.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.core.flow.context.DaemonContext;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class DaemonCheckerTask extends StackBasedStatusCheckerTask<DaemonContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DaemonCheckerTask.class);

    @Override
    public boolean checkStatus(DaemonContext daemonContext) {
        return daemonContext.getContainerOrchestrator().isBootstrapApiAvailable(daemonContext.getGatewayAddress());
    }

    @Override
    public void handleTimeout(DaemonContext t) {
        throw new InternalServerException("Operation timed out. Could not reach docker in time.");
    }

    @Override
    public String successMessage(DaemonContext t) {
        return "Docker is available.";
    }
}
