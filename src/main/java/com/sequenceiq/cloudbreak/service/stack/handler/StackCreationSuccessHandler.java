package com.sequenceiq.cloudbreak.service.stack.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientService;
import com.sequenceiq.cloudbreak.service.stack.event.StackCreationSuccess;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class StackCreationSuccessHandler implements Consumer<Event<StackCreationSuccess>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreationSuccessHandler.class);
    private static final String ADMIN = "admin";

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private Reactor reactor;

    @Autowired
    private AmbariClientService clientService;

    @Override
    public void accept(Event<StackCreationSuccess> event) {
        StackCreationSuccess stackCreationSuccess = event.getData();
        Long stackId = stackCreationSuccess.getStackId();
        String ambariIp = stackCreationSuccess.getAmbariIp();
        Stack stack = stackUpdater.updateAmbariIp(stackId, ambariIp);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Accepted {} event.", ReactorConfig.STACK_CREATE_SUCCESS_EVENT, stackId);
        String statusReason = "Cluster infrastructure and ambari are available on the cloud. AMBARI_IP:" + stack.getAmbariIp();
        stack = stackUpdater.updateStackStatus(stackId, Status.AVAILABLE, statusReason);
        stackUpdater.updateStackStatusReason(stack.getId(), "");
        changeAmbariCredentials(ambariIp, stack);
        LOGGER.info("Publishing {} event.", ReactorConfig.AMBARI_STARTED_EVENT);
        reactor.notify(ReactorConfig.AMBARI_STARTED_EVENT, Event.wrap(stack));
    }

    private void changeAmbariCredentials(String ambariIp, Stack stack) {
        String userName = stack.getUserName();
        String password = stack.getPassword();
        AmbariClient ambariClient = clientService.createDefault(ambariIp);
        if (ADMIN.equals(userName)) {
            if (!ADMIN.equals(password)) {
                ambariClient.changePassword(ADMIN, ADMIN, password, true);
            }
        } else {
            ambariClient.createUser(userName, password, true);
            ambariClient.deleteUser(ADMIN);
        }
    }

}
