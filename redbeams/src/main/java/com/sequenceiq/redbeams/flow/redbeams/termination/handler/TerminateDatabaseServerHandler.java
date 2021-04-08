package com.sequenceiq.redbeams.flow.redbeams.termination.handler;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists;
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate.TerminateDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate.TerminateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate.TerminateDatabaseServerSuccess;
import com.sequenceiq.redbeams.service.stack.DBResourceService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class TerminateDatabaseServerHandler extends ExceptionCatcherEventHandler<TerminateDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminateDatabaseServerHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Inject
    private EventBus eventBus;

    @Inject
    private DBResourceService dbResourceService;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(TerminateDatabaseServerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<TerminateDatabaseServerRequest> event) {
        TerminateDatabaseServerRequest request = event.getData();
        return new TerminateDatabaseServerFailed(resourceId, e, request.isForced());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<TerminateDatabaseServerRequest> event) {
        LOGGER.debug("Received event: {}", event);
        TerminateDatabaseServerRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<Object> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            if (request.getCloudCredential() != null) {
                AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
                List<CloudResource> resourcesToTerminate = dbResourceService.getAllAsCloudResource(request.getResourceId());
                List<CloudResourceStatus> resourceStatuses =
                        connector.resources()
                                .terminateDatabaseServer(
                                    ac,
                                    request.getDatabaseStack(),
                                    resourcesToTerminate,
                                    persistenceNotifier,
                                    request.isForced());
                List<CloudResource> resources = ResourceLists.transform(resourceStatuses);

                PollTask<ResourcesStatePollerResult> task = statusCheckFactory.newPollResourcesStateTask(ac, resources, true);
                ResourcesStatePollerResult statePollerResult = ResourcesStatePollerResults.build(cloudContext, resourceStatuses);
                if (!task.completed(statePollerResult)) {
                    statePollerResult = syncPollingScheduler.schedule(task);
                }
                LOGGER.debug("Terminating the database stack successfully finished for {}", cloudContext);
                return new TerminateDatabaseServerSuccess(request.getResourceId(), statePollerResult.getResults());
            } else {
                if (request.isForced()) {
                    return new TerminateDatabaseServerSuccess(request.getResourceId(), new ArrayList<>());
                } else {
                    return new TerminateDatabaseServerFailed(
                            request.getResourceId(),
                            new CloudbreakException("Could not detect cloud credential, probably the environment does not exist anymore"),
                            request.isForced());
                }
            }
        } catch (Exception e) {
            if (request.isForced()) {
                return new TerminateDatabaseServerSuccess(request.getResourceId(), new ArrayList<>());
            } else {
                LOGGER.warn("Error terminating the database stack:", e);
                return new TerminateDatabaseServerFailed(request.getResourceId(), e, request.isForced());
            }
        }
    }
}
