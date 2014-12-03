package com.sequenceiq.cloudbreak.service.stack.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientService;
import com.sequenceiq.cloudbreak.service.stack.event.StackCreationSuccess;
import com.sequenceiq.cloudbreak.service.stack.event.StackOperationFailure;

import reactor.core.Reactor;
import reactor.event.Event;

@Component
public class AmbariStartupListenerTask implements StatusCheckerTask<AmbariStartupPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariStartupListenerTask.class);

    @Autowired
    private Reactor reactor;

    @Autowired
    private AmbariClientService clientService;

    @Override
    public boolean checkStatus(AmbariStartupPollerObject aSPO) {
        boolean ambariRunning = false;
        AmbariClient ambariClient = clientService.createDefault(aSPO.getAmbariIp());
        LOGGER.info("Starting polling of Ambari server's status [Ambari server IP: '{}'].", aSPO.getAmbariIp());
        try {
            String ambariHealth = ambariClient.healthCheck();
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
        LOGGER.error("Unhandled exception occured while trying to reach initializing Ambari server.");
        LOGGER.info("Publishing {} event.", ReactorConfig.STACK_CREATE_FAILED_EVENT);
        StackOperationFailure stackCreationFailure = new StackOperationFailure(ambariStartupPollerObject.getStack().getId(),
                "Unhandled exception occured while trying to reach initializing Ambari server.");
        reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(stackCreationFailure));
    }

    @Override
    public String successMessage(AmbariStartupPollerObject aSPO) {
        LOGGER.info("Publishing {} event.", ReactorConfig.STACK_CREATE_SUCCESS_EVENT);
        reactor.notify(ReactorConfig.STACK_CREATE_SUCCESS_EVENT, Event.wrap(new StackCreationSuccess(aSPO.getStack().getId(), aSPO.getAmbariIp())));
        return "Ambari startup finished with success result.";
    }
}
