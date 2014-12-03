package com.sequenceiq.cloudbreak.service.stack.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;

@Service
public class AmbariStartupListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariStartupListener.class);

    private static final int POLLING_INTERVAL = 5000;
    private static final int MS_PER_SEC = 1000;
    private static final int SEC_PER_MIN = 60;
    private static final int MAX_POLLING_ATTEMPTS = SEC_PER_MIN / (POLLING_INTERVAL / MS_PER_SEC) * 10;


    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private AmbariStartupListenerTask ambariStartupListenerTask;
    @Autowired
    private PollingService<AmbariStartupPollerObject> ambariStartupPollerObjectPollingService;

    public void waitForAmbariServer(Long stackId, String ambariIp) {
        Stack stack = stackRepository.findById(stackId);
        MDCBuilder.buildMdcContext(stack);

        AmbariStartupPollerObject ambariStartupPollerObject = new AmbariStartupPollerObject(stack, ambariIp);
        ambariStartupPollerObjectPollingService.pollWithTimeout(ambariStartupListenerTask, ambariStartupPollerObject, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
    }

}
