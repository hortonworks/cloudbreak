package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists;
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

@Component
public class RemoveInstanceHandler implements CloudPlatformEventHandler<RemoveInstanceRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveInstanceHandler.class);

    @Inject
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<RemoveInstanceRequest> type() {
        return RemoveInstanceRequest.class;
    }

    @Override
    public void accept(Event<RemoveInstanceRequest> removeInstanceRequestEvent) {
        RemoveInstanceRequest request = removeInstanceRequestEvent.getData();
        RemoveInstanceResult result;
        try {
            CloudContext cloudContext = request.getCloudContext();
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudResourceStatus> resourceStatus = connector.resources().downscale(ac, request.getCloudStack(), request.getCloudResources(),
                    request.getInstances(), List.of());
            List<CloudResource> resources = ResourceLists.transform(resourceStatus);
            PollTask<ResourcesStatePollerResult> task = statusCheckFactory.newPollResourcesStateTask(ac, resources, true);
            ResourcesStatePollerResult statePollerResult = ResourcesStatePollerResults.build(cloudContext, resourceStatus);
            if (!task.completed(statePollerResult)) {
                statePollerResult = syncPollingScheduler.schedule(task);
            }
            LOGGER.debug("Instance remove successfully finished for {}", cloudContext);
            result = new RemoveInstanceResult(
                    new DownscaleStackResult(request.getResourceId(), ResourceLists.transform(statePollerResult.getResults())),
                    request.getResourceId(), request.getInstances());
        } catch (Exception e) {
            LOGGER.warn("Failed to handle RemoveInstanceRequest.", e);
            result = new RemoveInstanceResult(e.getMessage(), e, request.getResourceId(), request.getInstances());
        }
        eventBus.notify(result.selector(), new Event<>(removeInstanceRequestEvent.getHeaders(), result));
    }

}
