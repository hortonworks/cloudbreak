package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists;
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults;

import reactor.bus.Event;

@Component
public class TerminateStackHandler implements CloudPlatformEventHandler<TerminateStackRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminateStackHandler.class);
    private static final int INTERVAL = 5;
    private static final int MAX_ATTEMPT = 100;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Override
    public Class<TerminateStackRequest> type() {
        return TerminateStackRequest.class;
    }

    @Override
    public void accept(Event<TerminateStackRequest> terminateStackRequestEvent) {
        LOGGER.info("Received event: {}", terminateStackRequestEvent);
        TerminateStackRequest terminateStackRequest = terminateStackRequestEvent.getData();
        try {
            String platform = terminateStackRequest.getCloudContext().getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext ac = connector.authenticate(terminateStackRequest.getCloudContext(), terminateStackRequest.getCloudCredential());
            List<CloudResourceStatus> resourceStatus = connector.resources().terminate(ac, terminateStackRequest.getCloudResources());
            List<CloudResource> resources = ResourceLists.transform(resourceStatus);

            PollTask<ResourcesStatePollerResult> task = statusCheckFactory.newPollResourcesStateTask(ac, resources);
            ResourcesStatePollerResult statePollerResult = ResourcesStatePollerResults.build(terminateStackRequest.getCloudContext(), resourceStatus);
            if (!task.completed(statePollerResult)) {
                statePollerResult = syncPollingScheduler.schedule(task, INTERVAL, MAX_ATTEMPT);
            }

            TerminateStackResult terminateStackResult;
            if (!statePollerResult.getStatus().equals(ResourceStatus.DELETED)) {
                String statusReason = "Stack could not be terminated, Resource(s) could not be deleted on the provider side.";
                terminateStackResult = new TerminateStackResult(statusReason, null, terminateStackRequest);
            } else {
                terminateStackResult = new TerminateStackResult(terminateStackRequest);
            }
            terminateStackRequest.getResult().onNext(terminateStackResult);
        } catch (Exception e) {
            LOGGER.error("Failed to handle TerminateStackRequest: {}", e);
            terminateStackRequest.getResult().onNext(new TerminateStackResult("Stack termination failed.", e, terminateStackRequest));
        }
        LOGGER.info("TerminateStackHandler finished");

    }
}
