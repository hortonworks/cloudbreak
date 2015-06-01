package com.sequenceiq.cloudbreak.cloud.handler;


import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformConnectorV2;
import com.sequenceiq.cloudbreak.cloud.event.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.ListenablePollingScheduler;

import reactor.bus.Event;

@Component
public class LaunchStackHandler implements CloudPlatformEventHandler<LaunchStackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private Provider<ListenablePollingScheduler> listnablePollingScheduler;

    @Override
    public Class<LaunchStackRequest> type() {
        return LaunchStackRequest.class;
    }

    @Override
    public void accept(Event<LaunchStackRequest> launchStackRequestEvent) {
        LOGGER.info("Request received: {}", launchStackRequestEvent);
        LaunchStackRequest r = launchStackRequestEvent.getData();
        String platform = r.getStackContext().getPlatform();
        CloudPlatformConnectorV2 connector = cloudPlatformConnectors.get(platform);
        AuthenticatedContext authenticatedContext = connector.authenticate(r.getStackContext(), r.getCloudCredential());
        List<CloudResourceStatus> resources = connector.launchStack(authenticatedContext, r.getGroups(),
                r.getNetwork(), r.getSecurity(), r.getImage());
        ListenablePollingScheduler ph = listnablePollingScheduler.get();
        ph.schedule(10, 2);


        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        r.getResult().onNext("OK");

        //launchStackRequest.getResult().onError(new Exception());

        LOGGER.info("LaunchStackHandler finished");
    }


}
