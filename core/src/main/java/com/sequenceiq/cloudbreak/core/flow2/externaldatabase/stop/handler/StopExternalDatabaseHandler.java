package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.handler;

import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopEvent.EXTERNAL_DATABASE_STOPPED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopEvent.EXTERNAL_DATABASE_STOP_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_EXTERNAL_DATABASE_STOP_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_EXTERNAL_DATABASE_STOP_NOT_REQUIRED;

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
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StopExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StopExternalDatabaseRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StopExternalDatabaseResult;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseServerParameterDecorator;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.model.DatabaseType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class StopExternalDatabaseHandler extends ExceptionCatcherEventHandler<StopExternalDatabaseRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopExternalDatabaseHandler.class);

    @Inject
    private ExternalDatabaseService stopService;

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
        return "StopExternalDatabaseRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<StopExternalDatabaseRequest> event) {
        Stack stack = stackService.getById(event.getData().getResourceId());
        LOGGER.error(String.format("Exception during DB 'stop' for stack/cluster: %s", stack.getName()), e);
        return stopFailedEvent(stack, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<StopExternalDatabaseRequest> event) {
        LOGGER.debug("In StopExternalDatabaseHandler.doAccept");
        StopExternalDatabaseRequest request = event.getData();
        Stack stack = stackService.getById(request.getResourceId());
        DatabaseAvailabilityType externalDatabase = ObjectUtils.defaultIfNull(stack.getExternalDatabaseCreationType(), DatabaseAvailabilityType.NONE);
        LOGGER.debug("External database: {} for stack {}", externalDatabase.name(), stack.getName());
        LOGGER.debug("Getting environment CRN for stack {}", stack.getName());
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
        Selectable result;
        try {
            if (StackType.WORKLOAD != stack.getType()) {
                LOGGER.debug("External database stop in Cloudbreak service is required for WORKLOAD stacks only.");
                updateStackToStopped(stack, "External database stop is not required. Cluster stopped.",
                        CLUSTER_EXTERNAL_DATABASE_STOP_NOT_REQUIRED);
                result = new StopExternalDatabaseResult(stack.getId(), EXTERNAL_DATABASE_STOPPED_EVENT.event(), stack.getName(), null);
            } else if (externalDatabase.isEmbedded()) {
                LOGGER.info("External database for stack {} is not requested. Cluster stopped.", stack.getName());
                updateStackToStopped(stack, null, CLUSTER_EXTERNAL_DATABASE_STOP_NOT_REQUIRED);
                result = new StopExternalDatabaseResult(stack.getId(), EXTERNAL_DATABASE_STOPPED_EVENT.event(), stack.getName(), null);
            } else if (!isExternalDatabasePauseSupported(CloudPlatform.valueOf(environment.getCloudPlatform()), stack.getDatabase())) {
                LOGGER.debug("External database pause is not supported for '{}' cloud platform.", environment.getCloudPlatform());
                updateStackToStopped(stack, "External database stop is not supported by the cloud platform. Cluster stopped.",
                        CLUSTER_EXTERNAL_DATABASE_STOP_NOT_REQUIRED);
                result = new StopExternalDatabaseResult(stack.getId(), EXTERNAL_DATABASE_STOPPED_EVENT.event(), stack.getName(), null);
            } else {
                LOGGER.debug("Updating stack {} status from {} to {}",
                        stack.getName(), stack.getStatus().name(), DetailedStackStatus.EXTERNAL_DATABASE_STOP_IN_PROGRESS.name());
                stackUpdaterService.updateStatus(stack.getId(), DetailedStackStatus.EXTERNAL_DATABASE_STOP_IN_PROGRESS,
                        ResourceEvent.CLUSTER_EXTERNAL_DATABASE_STOP_COMMANCED, "External database stop in progress");
                stopService.stopDatabase(stack.getCluster(), externalDatabase, environment);
                updateStackToStopped(stack, "External database stop finished successfully", CLUSTER_EXTERNAL_DATABASE_STOP_FINISHED);
                result = new StopExternalDatabaseResult(stack.getId(), EXTERNAL_DATABASE_STOPPED_EVENT.event(),
                        stack.getName(), stack.getCluster().getDatabaseServerCrn());
            }
        } catch (UserBreakException e) {
            LOGGER.error("Database 'stop' polling exited before timeout. Cause: ", e);
            result = stopFailedEvent(stack, e);
        } catch (PollerStoppedException e) {
            LOGGER.error(String.format("Database 'stop' poller stopped for stack: %s", stack.getName()), e);
            result = stopFailedEvent(stack, e);
        } catch (PollerException e) {
            LOGGER.error(String.format("Database 'stop' polling failed for stack: %s", stack.getName()), e);
            result = stopFailedEvent(stack, e);
        }
        return result;
    }

    private void updateStackToStopped(Stack stack, String statusReason, ResourceEvent resourceEvent) {
        LOGGER.debug("Updating stack {} status from {} to {}", stack.getName(), stack.getStatus().name(), DetailedStackStatus.STOPPED.name());
        stackUpdaterService.updateStatus(stack.getId(), DetailedStackStatus.STOPPED, resourceEvent, statusReason);
    }

    private Selectable stopFailedEvent(Stack stack, Exception e) {
        return new StopExternalDatabaseFailed(stack.getId(), EXTERNAL_DATABASE_STOP_FAILED_EVENT.event(),
                stack.getName(), stack.getCluster().getDatabaseServerCrn(), e);
    }

    private boolean isExternalDatabasePauseSupported(CloudPlatform cloudPlatform, Database database) {
        DatabaseType databaseType = databaseServerParameterDecoratorMap.get(cloudPlatform).getDatabaseType(database.getAttributesMap()).orElse(null);
        return externalDatabaseConfig.isExternalDatabasePauseSupportedFor(cloudPlatform, databaseType);
    }
}
