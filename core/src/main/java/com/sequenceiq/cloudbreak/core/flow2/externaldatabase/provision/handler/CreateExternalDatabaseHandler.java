package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.handler;

import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationEvent.EXTERNAL_DATABASE_CREATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationEvent.EXTERNAL_DATABASE_WAIT_SUCCESS_EVENT;

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
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.CreateExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.CreateExternalDatabaseRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.CreateExternalDatabaseResult;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class CreateExternalDatabaseHandler implements EventHandler<CreateExternalDatabaseRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateExternalDatabaseHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ExternalDatabaseService provisionService;

    @Inject
    private StackUpdaterService stackUpdaterService;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private EnvironmentValidator environmentValidator;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return "CreateExternalDatabaseRequest";
    }

    @Override
    public void accept(Event<CreateExternalDatabaseRequest> createExternalDatabaseRequest) {
        LOGGER.debug("In CreateExternalDatabaseHandler.accept");
        CreateExternalDatabaseRequest request = createExternalDatabaseRequest.getData();
        Stack stack = stackService.getById(request.getResourceId());
        DatabaseAvailabilityType externalDatabase = ObjectUtils.defaultIfNull(stack.getExternalDatabaseCreationType(), DatabaseAvailabilityType.NONE);
        LOGGER.debug("External database: {} for stack {}", externalDatabase.name(), stack.getName());
        Selectable result;
        try {
            String resourceCrn = null;
            if (externalDatabase.isEmbedded()) {
                LOGGER.info("External database for stack {} is not requested.", stack.getName());
            } else {
                LOGGER.debug("Updating stack {} status from {} to {}",
                        stack.getName(), stack.getStatus().name(), DetailedStackStatus.EXTERNAL_DATABASE_CREATION_IN_PROGRESS.name());
                stackUpdaterService.updateStatus(stack.getId(), DetailedStackStatus.EXTERNAL_DATABASE_CREATION_IN_PROGRESS,
                        ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_STARTED, "External database creation in progress");
                LOGGER.debug("Getting environment CRN for stack {}", stack.getName());
                DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
                environmentValidator.checkValidEnvironment(stack.getName(), externalDatabase, environment);
                provisionService.provisionDatabase(stack, environment);
                LOGGER.debug("Updating stack {} status from {} to {}",
                        stack.getName(), stack.getStatus().name(), DetailedStackStatus.PROVISION_REQUESTED.name());
                stackUpdaterService.updateStatus(stack.getId(), DetailedStackStatus.PROVISION_REQUESTED,
                        ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_FINISHED, "External database creation finished");
                resourceCrn = stack.getCluster().getDatabaseServerCrn();
            }
            result = new CreateExternalDatabaseResult(stack.getId(), EXTERNAL_DATABASE_WAIT_SUCCESS_EVENT.event(), stack.getName(), resourceCrn);
        } catch (UserBreakException e) {
            LOGGER.info("Database polling exited before timeout. Cause: ", e);
            result = createFailedEvent(stack, e);
        } catch (PollerStoppedException e) {
            LOGGER.info(String.format("Database poller stopped for stack: %s", stack.getName()), e);
            result = createFailedEvent(stack, e);
        } catch (PollerException e) {
            LOGGER.info(String.format("Database polling failed for stack: %s", stack.getName()), e);
            result = createFailedEvent(stack, e);
        } catch (Exception e) {
            LOGGER.error(String.format("Exception during DB creation for stack/cluster: %s", stack.getName()), e);
            result = createFailedEvent(stack, e);
        }
        LOGGER.debug("Sending reactor notification for selector {}", result.selector());
        eventBus.notify(result.selector(), new Event<>(createExternalDatabaseRequest.getHeaders(), result));
    }

    private Selectable createFailedEvent(Stack stack, Exception e) {
        return new CreateExternalDatabaseFailed(stack.getId(), EXTERNAL_DATABASE_CREATION_FAILED_EVENT.event(), stack.getName(), null, e);
    }
}
