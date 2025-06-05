package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.handler;

import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartEvent.EXTERNAL_DATABASE_STARTED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartEvent.EXTERNAL_DATABASE_START_FAILED_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.conf.ExternalDatabaseConfig;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StartExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StartExternalDatabaseRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StartExternalDatabaseResult;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseServerParameterDecorator;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.model.DatabaseType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class StartExternalDatabaseHandler extends ExceptionCatcherEventHandler<StartExternalDatabaseRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartExternalDatabaseHandler.class);

    @Inject
    private ExternalDatabaseService startService;

    @Inject
    private StackUpdaterService stackUpdaterService;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private ExternalDatabaseConfig externalDatabaseConfig;

    @Inject
    private StackService stackService;

    @Inject
    private Map<CloudPlatform, DatabaseServerParameterDecorator> databaseServerParameterDecoratorMap;

    @Override
    public String selector() {
        return "StartExternalDatabaseRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<StartExternalDatabaseRequest> event) {
        Stack stack = stackService.getById(event.getData().getResourceId());
        LOGGER.error(String.format("Exception during DB 'start' for stack/cluster: %s", stack.getName()), e);
        return startFailedEvent(stack, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<StartExternalDatabaseRequest> event) {
        LOGGER.debug("In StartExternalDatabaseHandler.doAccept");
        StartExternalDatabaseRequest request = event.getData();
        Stack stack = stackService.getById(request.getResourceId());
        DatabaseAvailabilityType externalDatabase = ObjectUtils.defaultIfNull(stack.getExternalDatabaseCreationType(), DatabaseAvailabilityType.NONE);
        LOGGER.debug("External database: {} for stack {}", externalDatabase.name(), stack.getName());
        LOGGER.debug("Getting environment CRN for stack {}", stack.getName());
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
        Selectable result;
        try {
            if (StackType.WORKLOAD != stack.getType()) {
                LOGGER.debug("External database start in Cloudbreak service is required for WORKLOAD stacks only.");
                result = new StartExternalDatabaseResult(stack.getId(), EXTERNAL_DATABASE_STARTED_EVENT.event(),
                        stack.getName(), null);
            } else if (externalDatabase.isEmbedded()) {
                LOGGER.info("External database for stack {} is not requested. Start is not possible.", stack.getName());
                result = new StartExternalDatabaseResult(stack.getId(), EXTERNAL_DATABASE_STARTED_EVENT.event(),
                        stack.getName(), null);
            } else if (!isExternalDatabasePauseSupported(CloudPlatform.valueOf(environment.getCloudPlatform()), stack.getDatabase())) {
                LOGGER.debug("External database pause is not supported for '{}' cloud platform.", environment.getCloudPlatform());
                result = new StartExternalDatabaseResult(stack.getId(), EXTERNAL_DATABASE_STARTED_EVENT.event(),
                        stack.getName(), null);
            } else {
                LOGGER.debug("Updating stack {} status from {} to {}",
                        stack.getName(), stack.getStatus().name(), DetailedStackStatus.EXTERNAL_DATABASE_START_IN_PROGRESS.name());
                stackUpdaterService.updateStatus(stack.getId(), DetailedStackStatus.EXTERNAL_DATABASE_START_IN_PROGRESS,
                        ResourceEvent.CLUSTER_EXTERNAL_DATABASE_START_COMMANCED, "External database start in progress");
                startService.startDatabase(stack.getCluster(), externalDatabase, environment);
                LOGGER.debug("Updating stack {} status from {} to {}",
                        stack.getName(), stack.getStatus().name(), DetailedStackStatus.EXTERNAL_DATABASE_START_FINISHED.name());
                stackUpdaterService.updateStatus(stack.getId(), DetailedStackStatus.EXTERNAL_DATABASE_START_FINISHED,
                        ResourceEvent.CLUSTER_EXTERNAL_DATABASE_START_FINISHED, "External database start finished");
                result = new StartExternalDatabaseResult(stack.getId(), EXTERNAL_DATABASE_STARTED_EVENT.event(),
                        stack.getName(), stack.getCluster().getDatabaseServerCrn());
            }
        } catch (UserBreakException e) {
            LOGGER.error("Database 'start' polling exited before timeout. Cause: ", e);
            result = startFailedEvent(stack, e);
        } catch (PollerStoppedException e) {
            LOGGER.error(String.format("Database 'start' poller stopped for stack: %s", stack.getName()), e);
            result = startFailedEvent(stack, e);
        } catch (PollerException e) {
            LOGGER.error(String.format("Database 'start' polling failed for stack: %s", stack.getName()), e);
            result = startFailedEvent(stack, e);
        }
        return result;
    }

    private boolean isExternalDatabasePauseSupported(CloudPlatform cloudPlatform, Database database) {
        DatabaseType databaseType = databaseServerParameterDecoratorMap.get(cloudPlatform).getDatabaseType(database.getAttributesMap()).orElse(null);
        return externalDatabaseConfig.isExternalDatabasePauseSupportedFor(cloudPlatform, databaseType);
    }

    private Selectable startFailedEvent(Stack stack, Exception e) {
        return new StartExternalDatabaseFailed(stack.getId(), EXTERNAL_DATABASE_START_FAILED_EVENT.event(),
                stack.getName(), stack.getCluster().getDatabaseServerCrn(), e);
    }
}
