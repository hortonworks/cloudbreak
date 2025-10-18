package com.sequenceiq.redbeams.flow.redbeams.stop.handler;

import static com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus.STOPPED;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.stop.event.StopDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.stop.event.StopDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.stop.event.StopDatabaseServerSuccess;
import com.sequenceiq.redbeams.service.rotate.CloudProviderCertRotator;

@Component
public class StopDatabaseServerHandler implements EventHandler<StopDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopDatabaseServerHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<ExternalDatabaseStatus> externalDatabaseStatusPollingScheduler;

    @Inject
    private CloudProviderCertRotator cloudProviderCertRotator;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StopDatabaseServerRequest.class);
    }

    @Override
    public void accept(Event<StopDatabaseServerRequest> event) {
        LOGGER.debug("Received event: {}", event);
        StopDatabaseServerRequest request = event.getData();
        CloudCredential cloudCredential = request.getCloudCredential();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());

            DatabaseStack dbStack = request.getDbStack();
            ExternalDatabaseStatus status = connector.resources().getDatabaseServerStatus(ac, dbStack);
            String serverId = dbStack.getDatabaseServer().getServerId();
            if (status != null && status.isTransient()) {
                LOGGER.debug("Database server '{}' is in '{}' status. Start waiting for a permanent status.", serverId, status);
                PollTask<ExternalDatabaseStatus> task = statusCheckFactory.newPollPermanentExternalDatabaseStateTask(ac, dbStack);
                status = externalDatabaseStatusPollingScheduler.schedule(task);
            }

            if (status != STOPPED) {
                LOGGER.debug("Database server '{}' is in '{}' status. Calling for '{}' status.", serverId, status, STOPPED);
                cloudProviderCertRotator.rotate(
                        request.getResourceId(),
                        cloudContext,
                        cloudCredential,
                        dbStack,
                        false);
                connector.resources().stopDatabaseServer(ac, dbStack);
            } else {
                LOGGER.debug("Database server '{}' is already in '{}' status.", serverId, STOPPED);
            }

            RedbeamsEvent success = new StopDatabaseServerSuccess(request.getResourceId());
            eventBus.notify(success.selector(), new Event<>(event.getHeaders(), success));
            LOGGER.debug("Stopping the database server successfully finished for {}", cloudContext);
        } catch (Exception e) {
            StopDatabaseServerFailed failure = new StopDatabaseServerFailed(request.getResourceId(), e);
            LOGGER.warn("Error stopping the database server:", e);
            eventBus.notify(failure.selector(), new Event<>(event.getHeaders(), failure));
        }
    }
}
