package com.sequenceiq.redbeams.flow.redbeams.start.handler;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerSuccess;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.Event;
import reactor.bus.EventBus;

import static com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus.STARTED;

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
            CloudConnector<Object> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());

            ExternalDatabaseStatus status = connector.resources().getDatabaseServerStatus(ac, request.getDbInstanceIdentifier());
            if (status != null && status.isTransient()) {
                LOGGER.debug("Database server '{}' is in '{}' status. Start waiting for a permanent status.", request.getDbInstanceIdentifier(), status);

                PollTask<ExternalDatabaseStatus> task = statusCheckFactory.newPollPermanentExternalDatabaseStateTask(ac, request.getDbInstanceIdentifier());
                status = externalDatabaseStatusPollingScheduler.schedule(task);
            }

            if (status != STARTED) {
                LOGGER.debug("Database server '{}' is in '{}' status. Calling for '{}' status.",
                        request.getDbInstanceIdentifier(), status, STARTED);
                connector.resources().startDatabaseServer(ac, request.getDbInstanceIdentifier());
            } else {
                LOGGER.debug("Database server '{}' is already in '{}' status.", request.getDbInstanceIdentifier(), STARTED);
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
}
