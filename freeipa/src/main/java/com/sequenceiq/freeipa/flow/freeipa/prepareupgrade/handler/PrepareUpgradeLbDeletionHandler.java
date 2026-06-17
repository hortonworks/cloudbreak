package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.VALIDATION;

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
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeLbDeletionRequest;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeLbDeletionSuccess;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Component
public class PrepareUpgradeLbDeletionHandler extends ExceptionCatcherEventHandler<PrepareUpgradeLbDeletionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareUpgradeLbDeletionHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(PrepareUpgradeLbDeletionRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<PrepareUpgradeLbDeletionRequest> event) {
        LOGGER.error("Unexpected error during prepare upgrade LB deletion", e);
        return new PrepareUpgradeFailureEvent(resourceId, VALIDATION, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<PrepareUpgradeLbDeletionRequest> event) {
        PrepareUpgradeLbDeletionRequest request = event.getData();
        Long stackId = request.getResourceId();
        try {
            LOGGER.debug("Deleting temporary load balancer cloud resources for stack {}", stackId);
            CloudContext cloudContext = request.getCloudContext();
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatform(), cloudContext.getVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());

            List<CloudResource> lbResources = resourceService.findAllByStackId(stackId).stream()
                    .filter(r -> ResourceType.getAwsLbResourceTypes().contains(r.getResourceType()))
                    .map(resourceToCloudResourceConverter::convert)
                    .collect(Collectors.toList());

            if (!lbResources.isEmpty()) {
                connector.resources().terminate(ac, request.getCloudStack(), lbResources);
            }
            return new PrepareUpgradeLbDeletionSuccess(stackId);
        } catch (Exception e) {
            LOGGER.error("Failed to delete temporary load balancer for prepare upgrade", e);
            return new PrepareUpgradeFailureEvent(stackId, VALIDATION, e);
        }
    }
}
