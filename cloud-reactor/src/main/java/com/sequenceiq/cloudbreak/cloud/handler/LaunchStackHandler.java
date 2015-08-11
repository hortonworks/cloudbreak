package com.sequenceiq.cloudbreak.cloud.handler;


import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.ResourcePersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists;
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults;

import reactor.bus.Event;

@Component
public class LaunchStackHandler implements CloudPlatformEventHandler<LaunchStackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchStackHandler.class);

    private static final int INTERVAL = 5;
    private static final int MAX_ATTEMPT = 100;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private ResourcePersistenceNotifier resourcePersistenceNotifier;

    @Override
    public Class<LaunchStackRequest> type() {
        return LaunchStackRequest.class;
    }

    @Override
    public void accept(Event<LaunchStackRequest> launchStackRequestEvent) {
        LOGGER.info("Received event: {}", launchStackRequestEvent);
        LaunchStackRequest launchStackRequest = launchStackRequestEvent.getData();
        CloudContext cloudContext = launchStackRequest.getCloudContext();
        try {
            String platform = cloudContext.getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext ac = connector.authenticate(cloudContext, launchStackRequest.getCloudCredential());
            List<CloudResourceStatus> resourceStatus = connector.resources().launch(ac, launchStackRequest.getCloudStack(),
                    resourcePersistenceNotifier);
            List<CloudResource> resources = ResourceLists.transform(resourceStatus);
            PollTask<ResourcesStatePollerResult> task = statusCheckFactory.newPollResourcesStateTask(ac, resources);
            ResourcesStatePollerResult statePollerResult = ResourcesStatePollerResults.build(cloudContext, resourceStatus);
            if (!task.completed(statePollerResult)) {
                statePollerResult = syncPollingScheduler.schedule(task, INTERVAL, MAX_ATTEMPT);
            }
            launchStackRequest.getResult().onNext(ResourcesStatePollerResults.transformToLaunchStackResult(statePollerResult));
            LOGGER.info("Launching the stack successfully finished for {}", cloudContext);
        } catch (Exception e) {
            launchStackRequest.getResult().onNext(new LaunchStackResult(cloudContext, e));
        }
    }


}
