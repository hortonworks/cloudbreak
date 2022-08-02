package com.sequenceiq.redbeams.flow.redbeams.upgrade.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsUpgradeFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.UpgradeDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.UpgradeDatabaseServerSuccess;
import com.sequenceiq.redbeams.service.stack.DBStackService;

import reactor.bus.Event;

@Component
public class UpgradeDatabaseServerHandler extends ExceptionCatcherEventHandler<UpgradeDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeDatabaseServerHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeDatabaseServerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeDatabaseServerRequest> event) {
        RedbeamsUpgradeFailedEvent failure = new RedbeamsUpgradeFailedEvent(resourceId, e);
        LOGGER.warn("Error upgrading the database server:", e);
        return failure;
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeDatabaseServerRequest> handlerEvent) {
        Event<UpgradeDatabaseServerRequest> event = handlerEvent.getEvent();
        LOGGER.debug("Received event: {}", event);
        UpgradeDatabaseServerRequest request = event.getData();
        DatabaseStack databaseStack = request.getDatabaseStack();
        CloudCredential cloudCredential = request.getCloudCredential();
        CloudContext cloudContext = request.getCloudContext();
        CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
        Selectable response;
        try {
            connector.resources().upgradeDatabaseServer(ac, databaseStack, persistenceNotifier, request.getTargetMajorVersion());
            response = new UpgradeDatabaseServerSuccess(request.getResourceId());
            LOGGER.debug("Successfully upgraded the database server {}", databaseStack);
        } catch (Exception e) {
            response = new RedbeamsUpgradeFailedEvent(request.getResourceId(), e);
            LOGGER.warn("Error upgrading the database server {}:", databaseStack, e);
        }
        return response;
    }

}
