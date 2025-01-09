package com.sequenceiq.redbeams.flow.redbeams.upgrade.handler;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsValidateUpgradeCleanupFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerCleanupRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerCleanupSuccess;
import com.sequenceiq.redbeams.service.stack.DBResourceService;

@Component
public class ValidateUpgradeDatabaseServerCleanupHandler extends ExceptionCatcherEventHandler<ValidateUpgradeDatabaseServerCleanupRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateUpgradeDatabaseServerCleanupHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private DBResourceService dbResourceService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateUpgradeDatabaseServerCleanupRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ValidateUpgradeDatabaseServerCleanupRequest> event) {
        RedbeamsValidateUpgradeCleanupFailedEvent failure = new RedbeamsValidateUpgradeCleanupFailedEvent(resourceId, e);
        LOGGER.warn("Error during database server upgrade validation cleanup:", e);
        return failure;
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ValidateUpgradeDatabaseServerCleanupRequest> handlerEvent) {
        Event<ValidateUpgradeDatabaseServerCleanupRequest> event = handlerEvent.getEvent();
        LOGGER.debug("Received event: {}", event);
        ValidateUpgradeDatabaseServerCleanupRequest request = event.getData();
        DatabaseStack databaseStack = request.getDatabaseStack();
        CloudCredential cloudCredential = request.getCloudCredential();
        CloudContext cloudContext = request.getCloudContext();
        CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        Selectable response;
        Long dbStackId = request.getResourceId();
        List<CloudResource> cloudResources = dbResourceService.getAllAsCloudResource(dbStackId);

        try {
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
            ResourceConnector resourceConnector = connector.resources();
            resourceConnector.cleanupValidateUpgradeDatabaseServerResources(ac, databaseStack, cloudResources, persistenceNotifier);
            response = new ValidateUpgradeDatabaseServerCleanupSuccess(dbStackId);
        } catch (Exception ex) {
            LOGGER.warn("RDS upgrade validation cleanup failed on provider side", ex);
            response = new RedbeamsValidateUpgradeCleanupFailedEvent(dbStackId, ex);
        }
        return response;
    }
}