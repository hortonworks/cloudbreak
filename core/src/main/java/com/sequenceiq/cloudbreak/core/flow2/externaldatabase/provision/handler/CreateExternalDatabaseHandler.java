package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.handler;

import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationEvent.EXTERNAL_DATABASE_CREATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationEvent.EXTERNAL_DATABASE_WAIT_SUCCESS_EVENT;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.CreateExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.CreateExternalDatabaseRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.CreateExternalDatabaseResult;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

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
    private EnvironmentClientService environmentClientService;

    @Override
    public String selector() {
        return "CreateExternalDatabaseRequest";
    }

    @Override
    public void accept(Event<CreateExternalDatabaseRequest> createExternalDatabaseRequest) {
        LOGGER.debug("In CreateExternalDatabaseHandler.accept");
        CreateExternalDatabaseRequest request = createExternalDatabaseRequest.getData();
        Stack stack = request.getStack();
        DatabaseAvailabilityType externalDatabase = ObjectUtils.defaultIfNull(stack.getExternalDatabaseCreationType(), DatabaseAvailabilityType.NONE);
        LOGGER.debug("External database: {} for stack {}", externalDatabase.name(), stack.getName());
        Selectable result;
        try {
            if (externalDatabase == DatabaseAvailabilityType.NONE) {
                LOGGER.info("External database for stack {} is not requested.", stack.getName());
                result = new CreateExternalDatabaseResult(stack.getId(), EXTERNAL_DATABASE_WAIT_SUCCESS_EVENT.event(), stack.getName(), null);
            } else {
                LOGGER.debug("Updating stack {} status from {} to {}",
                        stack.getName(), stack.getStatus().name(), DetailedStackStatus.EXTERNAL_DATABASE_CREATION_IN_PROGRESS.name());
                stackUpdaterService.updateStatus(stack.getId(), DetailedStackStatus.EXTERNAL_DATABASE_CREATION_IN_PROGRESS,
                        ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_STARTED, "External database creation in progress");
                LOGGER.debug("Getting environment CRN for stack {}", stack.getName());
                DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
                checkValidEnvironment(stack.getName(), externalDatabase, environment);
                provisionService.provisionDatabase(stack.getCluster(), externalDatabase, environment);
                LOGGER.debug("Updating stack {} status from {} to {}",
                        stack.getName(), stack.getStatus().name(), DetailedStackStatus.PROVISION_REQUESTED.name());
                stackUpdaterService.updateStatus(stack.getId(), DetailedStackStatus.PROVISION_REQUESTED,
                        ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_FINISHED, "External database creation finished");
                result = new CreateExternalDatabaseResult(stack.getId(), EXTERNAL_DATABASE_WAIT_SUCCESS_EVENT.event(),
                        stack.getName(), stack.getCluster().getDatabaseServerCrn());
            }
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

    private void checkValidEnvironment(String stackName, DatabaseAvailabilityType externalDatabase, DetailedEnvironmentResponse environment) {
        LOGGER.debug("Checking environment validity for stack {}", stackName);
        if (CloudPlatform.AWS.name().equalsIgnoreCase(environment.getCloudPlatform())) {
            checkAwsEnvironment(stackName, externalDatabase, environment);
        }
    }

    private void checkAwsEnvironment(String stackName, DatabaseAvailabilityType externalDatabase, DetailedEnvironmentResponse environment) {
        if (externalDatabase == DatabaseAvailabilityType.HA) {
            LOGGER.info("Checking external HA Database prerequisites");
            String message;
            if (environment.getNetwork().getSubnetMetas().size() < 2) {
                message = String.format("Cannot create external HA database for stack: %s, not enough subnets in the vpc", stackName);
                LOGGER.debug(message);
                throw new BadRequestException(message);
            }

            Map<String, Long> zones = environment.getNetwork().getSubnetMetas().values().stream()
                    .collect(Collectors.groupingBy(CloudSubnet::getAvailabilityZone, Collectors.counting()));
            if (zones.size() < 2) {
                message = String.format("Cannot create external HA database for stack: %s, "
                        + "vpc subnets must cover at least two different availability zones", stackName);
                LOGGER.debug(message);
                throw new BadRequestException(message);
            }
            LOGGER.info("Prerequisites PASSED");
        }
    }

    private Selectable createFailedEvent(Stack stack, Exception e) {
        return new CreateExternalDatabaseFailed(stack.getId(), EXTERNAL_DATABASE_CREATION_FAILED_EVENT.event(), stack.getName(), null, e);
    }
}
