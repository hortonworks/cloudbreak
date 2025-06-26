package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists;
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

@Component
public class TerminateStackHandler implements CloudPlatformEventHandler<TerminateStackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminateStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<TerminateStackRequest> type() {
        return TerminateStackRequest.class;
    }

    @Override
    public void accept(Event<TerminateStackRequest> terminateStackRequestEvent) {
        LOGGER.debug("Received event: {}", terminateStackRequestEvent);
        TerminateStackRequest<TerminateStackResult> request = terminateStackRequestEvent.getData();
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
                    throw new CloudConnectorException("Stack could not be terminated, Resource(s) could not be deleted on the provider side.");
                } else {
                    result = new TerminateStackResult(request.getResourceId());
                }
            } else {
                result = new TerminateStackResult(request.getResourceId());
            }
            CloudCredentialStatus credentialStatus = connector.credentials().delete(ac);
            if (CredentialStatus.FAILED == credentialStatus.getStatus()) {
                if (credentialStatus.getException() != null) {
                    throw new CloudConnectorException(credentialStatus.getException());
                }
                throw new CloudConnectorException(credentialStatus.getStatusReason());
            }
            request.getResult().onNext(result);
            LOGGER.debug("TerminateStackHandler finished");
            eventBus.notify(result.selector(), new Event<>(terminateStackRequestEvent.getHeaders(), result));
        } catch (Exception e) {
            LOGGER.warn("Failed to handle TerminateStackRequest", e);
            TerminateStackResult terminateStackResult = new TerminateStackResult("Stack termination failed.", e, request.getResourceId());
            request.getResult().onNext(terminateStackResult);
            eventBus.notify(terminateStackResult.selector(), new Event<>(terminateStackRequestEvent.getHeaders(), terminateStackResult));
        }
    }
}
