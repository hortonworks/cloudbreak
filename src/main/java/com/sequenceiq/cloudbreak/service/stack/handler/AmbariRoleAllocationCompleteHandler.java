package com.sequenceiq.cloudbreak.service.stack.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientService;
import com.sequenceiq.cloudbreak.service.stack.event.AmbariRoleAllocationComplete;
import com.sequenceiq.cloudbreak.service.stack.event.StackCreationSuccess;
import com.sequenceiq.cloudbreak.service.stack.event.StackOperationFailure;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariStartupListenerTask;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariStartupPollerObject;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class AmbariRoleAllocationCompleteHandler implements Consumer<Event<AmbariRoleAllocationComplete>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariRoleAllocationCompleteHandler.class);
    private static final int POLLING_INTERVAL = 5000;
    private static final int MS_PER_SEC = 1000;
    private static final int SEC_PER_MIN = 60;
    private static final int MAX_POLLING_ATTEMPTS = SEC_PER_MIN / (POLLING_INTERVAL / MS_PER_SEC) * 10;

    @Autowired
    private Reactor reactor;
    @Autowired
    private AmbariClientService clientService;
    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private PollingService<AmbariStartupPollerObject> ambariStartupPollerObjectPollingService;
    @Autowired
    private AmbariStartupListenerTask ambariStartupListenerTask;


    @Override
    public void accept(Event<AmbariRoleAllocationComplete> event) {
        AmbariRoleAllocationComplete provisionSuccess = event.getData();
        MDCBuilder.buildMdcContext(provisionSuccess.getStack());
        Long stackId = provisionSuccess.getStack().getId();
        String ambariIp = provisionSuccess.getAmbariIp();
        LOGGER.info("Accepted {} event.", ReactorConfig.AMBARI_ROLE_ALLOCATION_COMPLETE_EVENT);
        Stack stack = stackRepository.findById(stackId);
        AmbariStartupPollerObject ambariStartupPollerObject = new AmbariStartupPollerObject(stack, ambariIp, clientService.createDefault(ambariIp));
        try {
            ambariStartupPollerObjectPollingService.pollWithTimeout(ambariStartupListenerTask,
                    ambariStartupPollerObject, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            LOGGER.info("Publishing {} event.", ReactorConfig.STACK_CREATE_SUCCESS_EVENT);
            reactor.notify(ReactorConfig.STACK_CREATE_SUCCESS_EVENT, Event.wrap(new StackCreationSuccess(stack.getId(), ambariIp)));
        } catch (Exception ex) {
            LOGGER.error("Timeout occured while trying to reach initializing Ambari server.");
            LOGGER.info("Publishing {} event.", ReactorConfig.STACK_CREATE_FAILED_EVENT);
            StackOperationFailure stackCreationFailure = new StackOperationFailure(ambariStartupPollerObject.getStack().getId(),
                    "Unhandled exception occured while trying to reach initializing Ambari server.");
            reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(stackCreationFailure));
        }

    }
}
