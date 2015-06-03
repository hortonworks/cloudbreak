package com.sequenceiq.cloudbreak.cloud.handler;


import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.event.StatusCheckRequest;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;

import reactor.bus.Event;

@Component
public class StatusCheckHandler implements CloudPlatformEventHandler<StatusCheckRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusCheckHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private Provider<SyncPollingScheduler> listenablePollingScheduler;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Override
    public Class<StatusCheckRequest> type() {
        return StatusCheckRequest.class;
    }


    @Override
    public void accept(Event<StatusCheckRequest> statusCheckRequestEvent) {

    }
}
