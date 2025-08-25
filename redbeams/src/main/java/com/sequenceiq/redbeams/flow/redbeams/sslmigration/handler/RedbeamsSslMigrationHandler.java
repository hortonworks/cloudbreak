package com.sequenceiq.redbeams.flow.redbeams.sslmigration.handler;

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
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationFailed;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationHandlerRequest;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationHandlerSuccessResult;

@Component
public class RedbeamsSslMigrationHandler extends ExceptionCatcherEventHandler<RedbeamsSslMigrationHandlerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsSslMigrationHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RedbeamsSslMigrationHandlerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RedbeamsSslMigrationHandlerRequest> event) {
        return new RedbeamsSslMigrationFailed(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<RedbeamsSslMigrationHandlerRequest> event) {
        LOGGER.debug("Received event: {}", event);
        RedbeamsSslMigrationHandlerRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        CloudCredential cloudCredential = request.getCloudCredential();
        DatabaseStack databaseStack = request.getDatabaseStack();
        Long resourceId = request.getResourceId();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(
                    cloudContext,
                    cloudCredential);

            ResourceConnector resources = connector.resources();
            ExternalDatabaseStatus status = resources.getDatabaseServerStatus(ac, databaseStack);
            if (status == ExternalDatabaseStatus.STARTED) {
                resources.migrateDatabaseFromNonSslToSsl(ac, databaseStack);
                LOGGER.debug("SSL Migration of the database server successfully finished for {}", cloudContext);
                return new RedbeamsSslMigrationHandlerSuccessResult(resourceId);
            } else {
                String errorMessage = String.format("Database server %s is not in started status.", databaseStack.getDatabaseServer().getServerId());
                LOGGER.warn(errorMessage);
                return new RedbeamsSslMigrationFailed(resourceId, new RedbeamsException(errorMessage));
            }
        } catch (Exception e) {
            LOGGER.warn("Error migrating the database server to use SSL:", e);
            return new RedbeamsSslMigrationFailed(resourceId, e);
        }
    }
}
