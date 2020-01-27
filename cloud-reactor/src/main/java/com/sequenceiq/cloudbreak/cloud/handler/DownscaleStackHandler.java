package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists;
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class DownscaleStackHandler implements CloudPlatformEventHandler<DownscaleStackRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DownscaleStackHandler.class);

    @Inject
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<DownscaleStackRequest> type() {
        return DownscaleStackRequest.class;
    }

    @Override
    public void accept(Event<DownscaleStackRequest> downscaleStackRequestEvent) {
        LOGGER.debug("Received event: {}", downscaleStackRequestEvent);
        DownscaleStackRequest request = downscaleStackRequestEvent.getData();
        DownscaleStackResult result;
        try {
            CloudContext cloudContext = request.getCloudContext();
            CloudConnector<Object> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudInstance> removableInstancesWithInstanceId = request.getInstances().stream()
                    .filter(cloudInstance -> cloudInstance.getInstanceId() != null)
                    .collect(Collectors.toList());
            List<CloudResourceStatus> resourceStatus = connector.resources().downscale(ac, request.getCloudStack(), request.getCloudResources(),
                    removableInstancesWithInstanceId, request.getResourcesToScale());
            List<CloudResource> resources = ResourceLists.transform(resourceStatus);
            PollTask<ResourcesStatePollerResult> task = statusCheckFactory.newPollResourcesStateTask(ac, resources, true);
            ResourcesStatePollerResult statePollerResult = ResourcesStatePollerResults.build(cloudContext, resourceStatus);
            if (!task.completed(statePollerResult)) {
                statePollerResult = syncPollingScheduler.schedule(task);
            }
            LOGGER.debug("Downscale successfully finished for {}", cloudContext);
            result = new DownscaleStackResult(request.getResourceId(), ResourceLists.transform(statePollerResult.getResults()));
        } catch (Exception e) {
            LOGGER.warn("Failed to handle DownscaleStackRequest.", e);
            result = new DownscaleStackResult(e.getMessage(), e, request.getResourceId());
        }
        request.getResult().onNext(result);
        eventBus.notify(result.selector(), new Event<>(downscaleStackRequestEvent.getHeaders(), result));
    }
}
