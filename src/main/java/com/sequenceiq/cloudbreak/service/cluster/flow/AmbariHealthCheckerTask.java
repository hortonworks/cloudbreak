package com.sequenceiq.cloudbreak.service.cluster.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.service.StackDependentStatusCheckerTask;

@Component
public class AmbariHealthCheckerTask extends StackDependentStatusCheckerTask<AmbariHealthCheckerPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariHealthCheckerTask.class);

    @Override
    public boolean checkStatus(AmbariHealthCheckerPollerObject ambariHealthCheckerPollerObject) {
        try {
            String ambariHealth = ambariHealthCheckerPollerObject.getAmbariClient().healthCheck();
            if ("RUNNING".equals(ambariHealth)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.info("Ambari is not running yet, polling");
            return false;
        }
    }

    @Override
    public void handleTimeout(AmbariHealthCheckerPollerObject t) {
        throw new InternalServerException(String.format("Operation timed out. Ambari server could not start %s", t.getAmbariClient().getAmbari().getUri()));
    }


    @Override
    public String successMessage(AmbariHealthCheckerPollerObject t) {
        return String.format("Ambari server successfully started '%s'", t.getAmbariClient().getAmbari().getUri());
    }

    @Override
    public void handleExit(AmbariHealthCheckerPollerObject ambariHealthCheckerPollerObject) {
        return;
    }

}
