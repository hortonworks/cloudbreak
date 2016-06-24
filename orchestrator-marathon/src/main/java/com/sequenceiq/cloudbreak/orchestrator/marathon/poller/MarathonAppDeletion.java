package com.sequenceiq.cloudbreak.orchestrator.marathon.poller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;

import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.utils.MarathonException;

public class MarathonAppDeletion implements OrchestratorBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarathonAppDeletion.class);
    private static final Integer STATUS_NOT_FOUND = 404;

    private final Marathon client;
    private final String appId;

    public MarathonAppDeletion(Marathon client, String appId) {
        this.client = client;
        this.appId = appId;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            client.getApp(appId).getApp();
            throw new CloudbreakOrchestratorFailedException(String.format("Marathon app '%s' hasn't been deleted yet.", appId));
        } catch (MarathonException me) {
            if (STATUS_NOT_FOUND.equals(me.getStatus())) {
                LOGGER.info("Marathon app has been deleted successfully with name: '{}'", appId);
            } else {
                throw new CloudbreakOrchestratorFailedException(me);
            }
        }
        return null;
    }
}
