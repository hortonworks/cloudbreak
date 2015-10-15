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
        TerminateStackRequest request = terminateStackRequestEvent.getData();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(request.getCloudContext(), request.getCloudCredential());
            List<CloudResourceStatus> resourceStatus = connector.resources().terminate(ac, request.getCloudStack(), request.getCloudResources());
            List<CloudResource> resources = ResourceLists.transform(resourceStatus);
            TerminateStackResult result;
            if (!resources.isEmpty()) {
                PollTask<ResourcesStatePollerResult> task = statusCheckFactory.newPollResourcesStateTask(ac, resources, false);
                ResourcesStatePollerResult statePollerResult = ResourcesStatePollerResults.build(request.getCloudContext(), resourceStatus);
                if (!task.completed(statePollerResult)) {
                    statePollerResult = syncPollingScheduler.schedule(task);
                }
                if (!statePollerResult.getStatus().equals(ResourceStatus.DELETED)) {
                    String statusReason = "Stack could not be terminated, Resource(s) could not be deleted on the provider side.";
                    result = new TerminateStackResult(statusReason, null, request);
                    LOGGER.info(statusReason);
                } else {
                    result = new TerminateStackResult(request);
                }
            } else {
                result = new TerminateStackResult(request);
            }
            connector.credentials().delete(ac);
            request.getResult().onNext(result);
            LOGGER.info("TerminateStackHandler finished");
        } catch (Exception e) {
            LOGGER.error("Failed to handle TerminateStackRequest: {}", e);
            request.getResult().onNext(new TerminateStackResult("Stack termination failed.", e, request));
        }
    }

}