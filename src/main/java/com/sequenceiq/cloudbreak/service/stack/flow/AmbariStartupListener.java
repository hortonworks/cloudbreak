package com.sequenceiq.cloudbreak.service.stack.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.Reactor;
import reactor.event.Event;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.service.cluster.AmbariHostsUnavailableException;
import com.sequenceiq.cloudbreak.service.stack.connector.aws.AwsStackUtil;
import com.sequenceiq.cloudbreak.service.stack.event.StackCreationFailure;
import com.sequenceiq.cloudbreak.service.stack.event.StackCreationSuccess;

@Service
public class AmbariStartupListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariStartupListener.class);

    private static final int POLLING_INTERVAL = 5000;
    private static final int MS_PER_SEC = 1000;
    private static final int SEC_PER_MIN = 60;
    private static final int MAX_POLLING_ATTEMPTS = SEC_PER_MIN / (POLLING_INTERVAL / MS_PER_SEC) * 10;

    @Autowired
    private Reactor reactor;

    @Autowired
    private AwsStackUtil awsStackUtil;

    public void waitForAmbariServer(Long stackId, String ambariIp) {
        try {
            boolean ambariRunning = false;
            AmbariClient ambariClient = new AmbariClient(ambariIp);
            int pollingAttempt = 0;
            LOGGER.info("Starting polling of Ambari server's status [stack: '{}', Ambari server IP: '{}'].", stackId, ambariIp);
            while (!ambariRunning && !(pollingAttempt >= MAX_POLLING_ATTEMPTS)) {
                try {
                    String ambariHealth = ambariClient.healthCheck();
                    LOGGER.info("Ambari health check returned: {} [stack: '{}', Ambari server IP: '{}']", ambariHealth, stackId, ambariIp);
                    if ("RUNNING".equals(ambariHealth)) {
                        ambariRunning = true;
                    }
                } catch (Exception e) {
                    LOGGER.info("Ambari health check failed. {} Trying again in next polling interval.", e.getMessage());
                }
                awsStackUtil.sleep(POLLING_INTERVAL);
                pollingAttempt++;
            }
            if (pollingAttempt >= MAX_POLLING_ATTEMPTS) {
                throw new AmbariHostsUnavailableException(String.format("Operation timed out. Failed to start Ambari server in %s seconds.",
                        MAX_POLLING_ATTEMPTS * POLLING_INTERVAL / MS_PER_SEC));
            }
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_CREATE_SUCCESS_EVENT, stackId);
            reactor.notify(ReactorConfig.STACK_CREATE_SUCCESS_EVENT, Event.wrap(new StackCreationSuccess(stackId, ambariIp)));
        } catch (Exception e) {
            LOGGER.error("Unhandled exception occured while trying to reach initializing Ambari server.", e);
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_CREATE_FAILED_EVENT, stackId);
            StackCreationFailure stackCreationFailure = new StackCreationFailure(stackId,
                    "Unhandled exception occured while trying to reach initializing Ambari server.");
            reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(stackCreationFailure));
        }
    }
}
