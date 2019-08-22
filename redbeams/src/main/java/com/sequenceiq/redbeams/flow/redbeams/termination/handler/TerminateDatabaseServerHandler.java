package com.sequenceiq.redbeams.flow.redbeams.termination.handler;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists;
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate.TerminateDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate.TerminateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate.TerminateDatabaseServerSuccess;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class TerminateDatabaseServerHandler implements EventHandler<TerminateDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminateDatabaseServerHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(TerminateDatabaseServerRequest.class);
    }

    @Override
    public void accept(Event<TerminateDatabaseServerRequest> event) {
        LOGGER.debug("Received event: {}", event);
        TerminateDatabaseServerRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<Object> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudResourceStatus> resourceStatuses =
                connector.resources().terminateDatabaseServer(ac, request.getDatabaseStack(), request.isForced());
            List<CloudResource> resources = ResourceLists.transform(resourceStatuses);

            PollTask<ResourcesStatePollerResult> task = statusCheckFactory.newPollResourcesStateTask(ac, resources, true);
            ResourcesStatePollerResult statePollerResult = ResourcesStatePollerResults.build(cloudContext, resourceStatuses);
            if (!task.completed(statePollerResult)) {
                statePollerResult = syncPollingScheduler.schedule(task);
            }
            RedbeamsEvent success = new TerminateDatabaseServerSuccess(request.getResourceId(), statePollerResult.getResults());
            // request.getResult().onNext(success);
            eventBus.notify(success.selector(), new Event<>(event.getHeaders(), success));
            LOGGER.debug("Terminating the database stack successfully finished for {}", cloudContext);
        } catch (Exception e) {
            TerminateDatabaseServerFailed failure = new TerminateDatabaseServerFailed(request.getResourceId(), e);
            LOGGER.warn("Error terminating the database stack:", e);
            // request.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(event.getHeaders(), failure));
        }
    }
}
