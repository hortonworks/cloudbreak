package com.sequenceiq.cloudbreak.service.stack.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.event.Event;
import reactor.function.Consumer;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.service.stack.event.AmbariRoleAllocationComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariStartupListener;

@Component
public class AmbariRoleAllocationCompleteHandler implements Consumer<Event<AmbariRoleAllocationComplete>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariRoleAllocationCompleteHandler.class);

    @Autowired
    private AmbariStartupListener ambariStartupListener;

    @Override
    public void accept(Event<AmbariRoleAllocationComplete> event) {
        AmbariRoleAllocationComplete provisionSuccess = event.getData();
        Long stackId = provisionSuccess.getStackId();
        String ambariIp = provisionSuccess.getAmbariIp();
        LOGGER.info("Accepted {} event.", ReactorConfig.AMBARI_ROLE_ALLOCATION_COMPLETE_EVENT, stackId);
        ambariStartupListener.waitForAmbariServer(stackId, ambariIp);
    }
}
