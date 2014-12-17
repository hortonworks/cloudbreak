package com.sequenceiq.cloudbreak.service.stack.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException;

@Component
@Scope("prototype")
public class AmbariStartupListenerTask implements StatusCheckerTask<AmbariStartupPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariStartupListenerTask.class);

    @Autowired
    private StackRepository stackRepository;

    @Override
    public boolean checkStatus(AmbariStartupPollerObject aSPO) {
        MDCBuilder.buildMdcContext(aSPO.getStack());
        boolean ambariRunning = false;
        LOGGER.info("Starting polling of Ambari server's status [Ambari server IP: '{}'].", aSPO.getAmbariIp());
        try {
            String ambariHealth = aSPO.getAmbariClient().healthCheck();
            LOGGER.info("Ambari health check returned: {} [Ambari server IP: '{}']", ambariHealth, aSPO.getAmbariIp());
            if ("RUNNING".equals(ambariHealth)) {
                ambariRunning = true;
            }
        } catch (Exception e) {
            LOGGER.info("Ambari health check failed. {} Trying again in next polling interval.", e.getMessage());
        }
        return ambariRunning;
    }

    @Override
    public void handleTimeout(AmbariStartupPollerObject ambariStartupPollerObject) {
        throw new AmbariOperationFailedException("Operation timed out. Failed to check ambari startup.");
    }

    @Override
    public boolean exitPoller(AmbariStartupPollerObject ambariStartupPollerObject) {
        try {
            stackRepository.findById(ambariStartupPollerObject.getStack().getId());
            return false;
        } catch (Exception ex) {
            return true;
        }
    }

    @Override
    public String successMessage(AmbariStartupPollerObject aSPO) {
        return "Ambari startup finished with success result.";
    }
}
