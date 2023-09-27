package com.sequenceiq.redbeams.flow.redbeams.provision.handler;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists;
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerSuccess;
import com.sequenceiq.redbeams.service.EnvironmentService;
import com.sequenceiq.redbeams.service.network.NetworkBuilderService;
import com.sequenceiq.redbeams.service.sslcertificate.DatabaseServerSslCertificatePrescriptionService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Component
public class AllocateDatabaseServerHandler extends ExceptionCatcherEventHandler<AllocateDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllocateDatabaseServerHandler.class);

    private static final String ERROR_FAILED_TO_LAUNCH_THE_DATABASE_STACK = "Failed to launch the database stack for %s due to: %s";

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Inject
    private DatabaseServerSslCertificatePrescriptionService databaseServerSslCertificatePrescriptionService;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private NetworkBuilderService networkBuilderService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private DBStackToDatabaseStackConverter dbStackToDatabaseStackConverter;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AllocateDatabaseServerRequest.class);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<AllocateDatabaseServerRequest> handlerEvent) {
        Event<AllocateDatabaseServerRequest> event = handlerEvent.getEvent();
        LOGGER.debug("Received event: {}", event);
        AllocateDatabaseServerRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        Selectable response;
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            CloudCredential cloudCredential = request.getCloudCredential();
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
            DBStack dbStack = dbStackService.getById(request.getResourceId());
            DatabaseStack databaseStack = setupNetworkIfMissing(request, dbStack);
            databaseServerSslCertificatePrescriptionService.prescribeSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);
            List<CloudResourceStatus> resourceStatuses = connector.resources().launchDatabaseServer(ac, databaseStack, persistenceNotifier);
            List<CloudResource> resources = ResourceLists.transform(resourceStatuses);

            PollTask<ResourcesStatePollerResult> task = statusCheckFactory.newPollResourcesStateTask(ac, resources, true);
            ResourcesStatePollerResult statePollerResult = ResourcesStatePollerResults.build(cloudContext, resourceStatuses);
            if (!task.completed(statePollerResult)) {
                statePollerResult = syncPollingScheduler.schedule(task);
            }
            validateResourcesState(cloudContext, statePollerResult);
            response = new AllocateDatabaseServerSuccess(request.getResourceId());
            LOGGER.debug("Launching the database stack successfully finished for {}", cloudContext);
        } catch (Exception e) {
            response = new AllocateDatabaseServerFailed(request.getResourceId(), e);
            LOGGER.warn("Error launching the database stack:", e);
        }
        return response;
    }

    private DatabaseStack setupNetworkIfMissing(AllocateDatabaseServerRequest request, DBStack dbStack) {
        DatabaseStack databaseStack = request.getDatabaseStack();
        if (dbStack.getNetwork() == null) {
            LOGGER.debug("Network is missing for DBStack, setting up");
            DetailedEnvironmentResponse environment = environmentService.getByCrn(dbStack.getEnvironmentId());
            dbStack.setNetwork(networkBuilderService.buildNetwork(request.getNetworkParameters(), environment, dbStack).getId());
            dbStackService.save(dbStack);
            return new DatabaseStack(dbStackToDatabaseStackConverter.buildNetwork(dbStack), databaseStack.getDatabaseServer(), databaseStack.getTags(),
                    databaseStack.getTemplate());
        } else {
            return databaseStack;
        }
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<AllocateDatabaseServerRequest> event) {
        AllocateDatabaseServerFailed failure = new AllocateDatabaseServerFailed(resourceId, e);
        LOGGER.warn("Error launching the database stack:", e);
        return failure;
    }

    private void validateResourcesState(CloudContext cloudContext, ResourcesStatePollerResult statePollerResult) {
        if (statePollerResult == null || statePollerResult.getResults() == null) {
            throw new IllegalStateException(String.format("%s is null, cannot check launch status of database stack for %s",
                    statePollerResult == null ? "ResourcesStatePollerResult" : "ResourcesStatePollerResult.results", cloudContext));
        }

        List<CloudResourceStatus> results = statePollerResult.getResults();
        if (results.size() == 1 && (results.get(0).isFailed() || results.get(0).isDeleted())) {
            throw new OperationException(String.format(ERROR_FAILED_TO_LAUNCH_THE_DATABASE_STACK, cloudContext, results.get(0).getStatusReason()));
        }
        List<CloudResourceStatus> failedResources = results.stream()
                .filter(r -> r.isFailed() || r.isDeleted())
                .collect(Collectors.toList());
        if (!failedResources.isEmpty()) {
            throw new OperationException(String.format(ERROR_FAILED_TO_LAUNCH_THE_DATABASE_STACK, cloudContext, failedResources));
        }
    }

}
