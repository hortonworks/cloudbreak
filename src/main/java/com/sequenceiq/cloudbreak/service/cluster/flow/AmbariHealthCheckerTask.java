package com.sequenceiq.cloudbreak.service.cluster.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;

public class AmbariHealthCheckerTask implements StatusCheckerTask<AmbariClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariHealthCheckerTask.class);

    @Override
    public boolean checkStatus(AmbariClient ambariClient) {
        try {
            String ambariHealth = ambariClient.healthCheck();
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
    public void handleTimeout(AmbariClient t) {
        throw new InternalServerException(String.format("Operation timed out. Ambari server could not start %s", t.getAmbari().getUri()));
    }

    @Override
    public String successMessage(AmbariClient t) {
        return String.format("Ambari server successfully started '%s'", t.getAmbari().getUri());
    }

}
