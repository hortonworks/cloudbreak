package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.handler;

import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationEvent.EXTERNAL_DATABASE_TERMINATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationEvent.EXTERNAL_DATABASE_WAIT_TERMINATION_SUCCESS_EVENT;

import jakarta.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.TerminateExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.TerminateExternalDatabaseRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.TerminateExternalDatabaseResult;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class TerminateExternalDatabaseHandler implements EventHandler<TerminateExternalDatabaseRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminateExternalDatabaseHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ExternalDatabaseService terminationService;

    @Inject
    private StackUpdaterService stackUpdaterService;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return "TerminateExternalDatabaseRequest";
    }

    @Override
    public void accept(Event<TerminateExternalDatabaseRequest> terminateExternalDatabaseRequest) {
        LOGGER.debug("In TerminateExternalDatabaseHandler.accept");
        TerminateExternalDatabaseRequest request = terminateExternalDatabaseRequest.getData();
        Stack stack = stackService.getById(request.getResourceId());
        DatabaseAvailabilityType externalDatabase = ObjectUtils.defaultIfNull(stack.getExternalDatabaseCreationType(), DatabaseAvailabilityType.NONE);
        LOGGER.debug("External database: {} for stack {}", externalDatabase.name(), stack.getName());
        Selectable result;
        try {
            if (externalDatabase.isEmbedded()) {
                LOGGER.info("External database for stack {} is not requested. Termination is not necessary.", stack.getName());
                result = new TerminateExternalDatabaseResult(stack.getId(), EXTERNAL_DATABASE_WAIT_TERMINATION_SUCCESS_EVENT.event(),
                        stack.getName(), null);

            } else {
                LOGGER.debug("Updating stack {} status from {} to {}",
                        stack.getName(), stack.getStatus().name(), DetailedStackStatus.EXTERNAL_DATABASE_DELETION_IN_PROGRESS.name());
                stackUpdaterService.updateStatus(stack.getId(), DetailedStackStatus.EXTERNAL_DATABASE_DELETION_IN_PROGRESS,
                        ResourceEvent.CLUSTER_EXTERNAL_DATABASE_DELETION_STARTED, "External database deletion in progress");
                LOGGER.debug("Getting environment CRN for stack {}", stack.getName());
                DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
                terminationService.terminateDatabase(stack.getCluster(), externalDatabase, environment, request.isForced());
                LOGGER.debug("Updating stack {} status from {} to {}",
                        stack.getName(), stack.getStatus().name(), DetailedStackStatus.AVAILABLE.name());
                stackUpdaterService.updateStatus(stack.getId(), DetailedStackStatus.AVAILABLE,
                        ResourceEvent.CLUSTER_EXTERNAL_DATABASE_DELETION_FINISHED, "External database deletion finished");
                result = new TerminateExternalDatabaseResult(stack.getId(), EXTERNAL_DATABASE_WAIT_TERMINATION_SUCCESS_EVENT.event(),
                        stack.getName(), stack.getCluster().getDatabaseServerCrn());
            }
        } catch (UserBreakException e) {
            LOGGER.info("Database termination polling exited before timeout. Cause: ", e);
            result = terminateFailedEvent(stack, e);
        } catch (PollerStoppedException e) {
            LOGGER.info(String.format("Database termination poller stopped for stack: %s", stack.getName()), e);
            result = terminateFailedEvent(stack, e);
        } catch (PollerException e) {
            LOGGER.info(String.format("Database termination polling failed for stack: %s", stack.getName()), e);
            result = terminateFailedEvent(stack, e);
        } catch (Exception e) {
            LOGGER.error(String.format("Exception during DB termination for stack/cluster: %s", stack.getName()), e);
            result = terminateFailedEvent(stack, e);
        }
        LOGGER.debug("Sending reactor notification for selector {}", result.selector());
        eventBus.notify(result.selector(), new Event<>(terminateExternalDatabaseRequest.getHeaders(), result));
    }

    private Selectable terminateFailedEvent(Stack stack, Exception e) {
        return new TerminateExternalDatabaseFailed(stack.getId(), EXTERNAL_DATABASE_TERMINATION_FAILED_EVENT.event(), stack.getName(), null, e);
    }
}
