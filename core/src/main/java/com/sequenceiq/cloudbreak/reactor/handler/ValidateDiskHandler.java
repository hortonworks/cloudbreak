package com.sequenceiq.cloudbreak.reactor.handler;

import static java.lang.String.format;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ValidateDiskRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ValidateDiskResult;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ValidateDiskHandler extends ExceptionCatcherEventHandler<ValidateDiskRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateDiskHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateDiskRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ValidateDiskRequest> event) {
        LOGGER.error("Unexpected error happened during volume validation.", e);
        return new ValidateDiskResult(e, resourceId);
    }

    @Override
    public Selectable doAccept(HandlerEvent<ValidateDiskRequest> event) {
        LOGGER.debug("Received event: {}", event);
        ValidateDiskRequest<ValidateDiskResult> request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        Selectable result;
        try {
            CloudConnector connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());

            List<CloudResourceStatus> cloudResourceStates = connector.resources().checkForSyncer(ac, request.getCloudResources());

            List<CloudResourceStatus> deletedResources = cloudResourceStates.stream()
                    .filter(CloudResourceStatus::isDeleted)
                    .toList();
            if (deletedResources.isEmpty()) {
                result = validCase(event);
            } else {
                result = errorCase(event, new CloudbreakRuntimeException(
                        format("Cannot repair the nodes because one or more disks are missing from node(s): %s, " +
                                        "please repair the node(s) with the 'Delete volumes' option, this will create new disks for the instance. " +
                                        "Please note that 'Delete volumes' will remove all the attached disks and can cause data loss.",
                                getAllInstanceIdSafely(deletedResources))));
            }
        } catch (UnsupportedOperationException e) {
            LOGGER.debug(format("Call checkForSyncer is not supported for %s platform, %s variant. Error message: %s",
                    cloudContext.getPlatform().value(), cloudContext.getVariant().value(), e.getMessage()));
            result = validCase(event);
        }
        return result;
    }

    private String getAllInstanceIdSafely(List<CloudResourceStatus> cloudResourceStatuses) {
        return cloudResourceStatuses.stream()
                .map(CloudResourceStatus::getCloudResource)
                .map(CloudResource::getInstanceId)
                .collect(Collectors.joining(","));
    }

    private Selectable validCase(HandlerEvent<ValidateDiskRequest> event) {
        ValidateDiskRequest<ValidateDiskResult> request = event.getData();
        ValidateDiskResult result = new ValidateDiskResult(request.getResourceId());
        request.getResult().onNext(result);
        LOGGER.debug("Validate disk finished for {}", request.getCloudContext());
        return result;
    }

    private Selectable errorCase(HandlerEvent<ValidateDiskRequest> event, RuntimeException e) {
        ValidateDiskRequest<ValidateDiskResult> request = event.getData();
        ValidateDiskResult failure = new ValidateDiskResult(e, request.getResourceId());
        request.getResult().onNext(failure);
        LOGGER.debug("Error during disk validation.", e);
        return failure;
    }
}
