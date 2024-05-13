package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudImageException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists;
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

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
        CloudStack cloudStack = request.getCloudStack();
        CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
        try {
            List<CloudResourceStatus> resourceStatus = connector.resources().launch(ac, cloudStack, persistenceNotifier,
                    request.getAdjustmentWithThreshold());
            ResourcesStatePollerResult statePollerResult = waitForResources(ac, resourceStatus, cloudContext);
            LaunchStackResult result = ResourcesStatePollerResults.transformToLaunchStackResult(request, statePollerResult);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(launchStackRequestEvent.getHeaders(), result));
            LOGGER.debug("Launching the stack successfully finished for {}", cloudContext);
        } catch (CloudImageException e) {
            if (request.getFallbackImage().isPresent()) {
                LaunchStackResult result = new LaunchStackResult(request.getResourceId(), List.of());
                request.getResult().onNext(result);
                eventBus.notify("IMAGEFALLBACK", new Event<>(launchStackRequestEvent.getHeaders(), result));
                LOGGER.debug("Marketplace image error, attempt to fallback to vhd image {}", cloudContext);
            } else {
                LOGGER.debug("There is no fallback image available for launching stack, re-submitting original exception {}", e.getMessage());
                failLaunchFlow(launchStackRequestEvent, e, request);
            }
        } catch (Exception e) {
            if (ExceptionUtils.getRootCause(e) instanceof InterruptedException) {
                LOGGER.info("Interrupted exception is ignored as it has been thrown because of graceful shutdown of the java process.");
            }
            LOGGER.warn("Error during launching the stack:", e);
            failLaunchFlow(launchStackRequestEvent, e, request);
        }
    }

    private void failLaunchFlow(Event<LaunchStackRequest> launchStackRequestEvent, Exception e, LaunchStackRequest request) {
        LaunchStackResult failure = new LaunchStackResult(e, request.getResourceId());

        request.getResult().onNext(failure);
        eventBus.notify(failure.selector(), new Event<>(launchStackRequestEvent.getHeaders(), failure));
    }

    /**
     * Creates a poll task which waits for the resources to be fully up and running, or more specifically,
     * for the resources to be in a permanent successful state.
     * <p>
     * Returns the result of the poll task.
     */
    private ResourcesStatePollerResult waitForResources(AuthenticatedContext ac, List<CloudResourceStatus> resourceStatuses, CloudContext cloudContext)
            throws Exception {
        List<CloudResource> resources = ResourceLists.transform(resourceStatuses);
        PollTask<ResourcesStatePollerResult> task = statusCheckFactory.newPollResourcesStateTask(ac, resources, true);
        ResourcesStatePollerResult statePollerResult = ResourcesStatePollerResults.build(cloudContext, resourceStatuses);
        if (!task.completed(statePollerResult)) {
            statePollerResult = syncPollingScheduler.schedule(task);
        }
        return statePollerResult;
    }
}