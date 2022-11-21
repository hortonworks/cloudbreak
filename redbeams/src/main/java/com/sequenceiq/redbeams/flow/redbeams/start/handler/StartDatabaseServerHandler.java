package com.sequenceiq.redbeams.flow.redbeams.start.handler;

import static com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus.STARTED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.redbeams.exception.DatabaseStartFailedException;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerSuccess;

@Component
public class StartDatabaseServerHandler implements EventHandler<StartDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartDatabaseServerHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<ExternalDatabaseStatus> externalDatabaseStatusPollingScheduler;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StartDatabaseServerRequest.class);
    }

    @Override
    public void accept(Event<StartDatabaseServerRequest> event) {
        LOGGER.debug("Received event: {}", event);
        StartDatabaseServerRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());

            ExternalDatabaseStatus status = connector.resources().getDatabaseServerStatus(ac, request.getDbStack());
            if (status != null && status.isTransient()) {
                LOGGER.debug("Database server '{}' is in '{}' status. Start waiting for a permanent status.", request.getDbStack(), status);
                status = pollAndGetDatabaseStatus(ac, request.getDbStack());
            }

            if (status != STARTED) {
                LOGGER.debug("Database server '{}' is in '{}' status. Calling for '{}' status.",
                        request.getDbStack(), status, STARTED);
                connector.resources().startDatabaseServer(ac, request.getDbStack());
                status = pollAndGetDatabaseStatus(ac, request.getDbStack());
            } else {
                LOGGER.debug("Database server '{}' is already in '{}' status.", request.getDbStack(), STARTED);
            }

            if (STARTED != status) {
                throw new DatabaseStartFailedException("Unable to start database server!");
            }

            RedbeamsEvent success = new StartDatabaseServerSuccess(request.getResourceId());
            eventBus.notify(success.selector(), new Event<>(event.getHeaders(), success));
            LOGGER.debug("Starting the database server successfully finished for {}", cloudContext);
        } catch (Exception e) {
            StartDatabaseServerFailed failure = new StartDatabaseServerFailed(request.getResourceId(), e);
            LOGGER.warn("Error starting the database server:", e);
            eventBus.notify(failure.selector(), new Event<>(event.getHeaders(), failure));
        }
    }

    private ExternalDatabaseStatus pollAndGetDatabaseStatus(AuthenticatedContext ac, DatabaseStack dbStack) throws Exception {
        PollTask<ExternalDatabaseStatus> task = statusCheckFactory.newPollPermanentExternalDatabaseStateTask(ac, dbStack);
        LOGGER.debug("About to poll database for permanent status, since its current is: {}",
                dbStack.getDatabaseServer() != null ? dbStack.getDatabaseServer().getStatus() : "unknown");
        return externalDatabaseStatusPollingScheduler.schedule(task);
    }

}
