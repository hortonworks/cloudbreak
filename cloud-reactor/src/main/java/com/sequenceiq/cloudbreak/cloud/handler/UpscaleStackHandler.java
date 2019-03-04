package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
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
public class UpscaleStackHandler implements CloudPlatformEventHandler<UpscaleStackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpscaleStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<UpscaleStackRequest> type() {
        return UpscaleStackRequest.class;
    }

    @Override
    public void accept(Event<UpscaleStackRequest> upscaleStackRequestEvent) {
        LOGGER.debug("Received event: {}", upscaleStackRequestEvent);
        UpscaleStackRequest<UpscaleStackResult> request = upscaleStackRequestEvent.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<?> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudResourceStatus> resourceStatus = connector.resources().upscale(ac, request.getCloudStack(), request.getResourceList());
            List<CloudResource> resources = ResourceLists.transform(resourceStatus);
            PollTask<ResourcesStatePollerResult> task = statusCheckFactory.newPollResourcesStateTask(ac, resources, true);
            ResourcesStatePollerResult statePollerResult = ResourcesStatePollerResults.build(cloudContext, resourceStatus);
            if (!task.completed(statePollerResult)) {
                statePollerResult = syncPollingScheduler.schedule(task);
            }
            UpscaleStackResult result = ResourcesStatePollerResults.transformToUpscaleStackResult(statePollerResult, request);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(upscaleStackRequestEvent.getHeaders(), result));
            LOGGER.debug("Upscale successfully finished for {}", cloudContext);
        } catch (Exception e) {
            UpscaleStackResult result = new UpscaleStackResult(e.getMessage(), e, request);
            request.getResult().onNext(result);
            eventBus.notify(CloudPlatformResult.failureSelector(UpscaleStackResult.class), new Event<>(upscaleStackRequestEvent.getHeaders(), result));
        }
    }

}
