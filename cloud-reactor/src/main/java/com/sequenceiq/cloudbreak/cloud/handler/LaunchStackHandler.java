package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists;
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class LaunchStackHandler implements CloudPlatformEventHandler<LaunchStackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<LaunchStackRequest> type() {
        return LaunchStackRequest.class;
    }

    @Override
    public void accept(Event<LaunchStackRequest> launchStackRequestEvent) {
        LOGGER.debug("Received event: {}", launchStackRequestEvent);
        LaunchStackRequest request = launchStackRequestEvent.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<Object> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudResourceStatus> resourceStatus = connector.resources().launch(ac, request.getCloudStack(), persistenceNotifier,
                    request.getAdjustmentType(), request.getThreshold());
            List<CloudResource> resources = ResourceLists.transform(resourceStatus);
            PollTask<ResourcesStatePollerResult> task = statusCheckFactory.newPollResourcesStateTask(ac, resources, true);
            ResourcesStatePollerResult statePollerResult = ResourcesStatePollerResults.build(cloudContext, resourceStatus);
            if (!task.completed(statePollerResult)) {
                statePollerResult = syncPollingScheduler.schedule(task);
            }
            LaunchStackResult result = ResourcesStatePollerResults.transformToLaunchStackResult(request, statePollerResult);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(launchStackRequestEvent.getHeaders(), result));
            LOGGER.debug("Launching the stack successfully finished for {}", cloudContext);
        } catch (Exception e) {
            LaunchStackResult failure = new LaunchStackResult(e, request);
            LOGGER.warn("Error during launching the stack:", e);
            request.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(launchStackRequestEvent.getHeaders(), failure));
        }
    }
}
