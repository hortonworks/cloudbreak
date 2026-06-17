package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.handler;

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
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureCleanupComplete;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureCleanupRequest;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Component
public class PrepareUpgradeFailureCleanupHandler extends ExceptionCatcherEventHandler<PrepareUpgradeFailureCleanupRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareUpgradeFailureCleanupHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(PrepareUpgradeFailureCleanupRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<PrepareUpgradeFailureCleanupRequest> event) {
        LOGGER.error("Unexpected error during prepare upgrade failure cleanup", e);
        return new PrepareUpgradeFailureCleanupComplete(resourceId);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<PrepareUpgradeFailureCleanupRequest> event) {
        PrepareUpgradeFailureCleanupRequest request = event.getData();
        Long stackId = request.getResourceId();
        try {
            LOGGER.debug("Attempting best-effort cloud LB resource cleanup for stack {}", stackId);
            CloudContext cloudContext = request.getCloudContext();
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatform(), cloudContext.getVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());

            List<CloudResource> lbResources = resourceService.findAllByStackId(stackId).stream()
                    .filter(r -> ResourceType.getAwsLbResourceTypes().contains(r.getResourceType()))
                    .map(resourceToCloudResourceConverter::convert)
                    .collect(Collectors.toList());

            if (!lbResources.isEmpty()) {
                connector.resources().terminate(ac, request.getCloudStack(), lbResources);
                LOGGER.debug("Successfully terminated cloud LB resources during failure cleanup for stack {}", stackId);
            }
        } catch (Exception e) {
            LOGGER.warn("Best-effort cloud LB resource cleanup failed for stack {}. Resources may be orphaned.", stackId, e);
        }
        return new PrepareUpgradeFailureCleanupComplete(stackId);
    }
}
