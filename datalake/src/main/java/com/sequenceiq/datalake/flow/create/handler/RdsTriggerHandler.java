package com.sequenceiq.datalake.flow.create.handler;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.create.event.RdsTriggerRequest;
import com.sequenceiq.datalake.flow.create.event.RdsTriggeredEvent;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RdsTriggerHandler extends ExceptionCatcherEventHandler<RdsTriggerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsTriggerHandler.class);

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private DatabaseService databaseService;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private RdsCreationHandlerSupport rdsCreationHandlerSupport;

    @Value("${sdx.parallel.rds.create.enabled:false}")
    private boolean parallelRdsCreateEnabled;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RdsTriggerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RdsTriggerRequest> event) {
        return new SdxCreateFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RdsTriggerRequest> event) {
        RdsTriggerRequest rdsTriggerRequest = event.getData();
        Long sdxId = rdsTriggerRequest.getResourceId();
        String userId = rdsTriggerRequest.getUserId();
        try {
            DetailedEnvironmentResponse environment = environmentService.waitNetworkAndGetEnvironment(sdxId);
            Optional<SdxCluster> sdxCluster = sdxClusterRepository.findById(sdxId);
            if (sdxCluster.isPresent()) {
                if (sdxCluster.get().hasExternalDatabase()) {
                    rdsCreationHandlerSupport.validForDatabaseCreation(sdxId, environment);
                    if (parallelRdsCreateEnabled) {
                        LOGGER.debug("Parallel RDS create enabled - firing non-blocking createInternal for Data Lake: {}", sdxId);
                        databaseService.fireCreate(sdxCluster.get(), environment);
                    } else {
                        LOGGER.debug("Parallel RDS create disabled - polling database for Data Lake: {}", sdxId);
                        databaseService.createAndWaitUntilAvailable(sdxCluster.get(), environment);
                        rdsCreationHandlerSupport.setRdsCreatedStatus(sdxCluster.get());
                    }
                    return new RdsTriggeredEvent(sdxId, userId);
                } else {
                    LOGGER.debug("skipping creation of database for Data Lake: {}", sdxId);
                    return new RdsTriggeredEvent(sdxId, userId);
                }
            } else {
                throw notFound("Data Lake cluster", sdxId).get();
            }
        } catch (Exception exception) {
            return rdsCreationHandlerSupport.toFailedEvent(sdxId, userId, exception);
        }
    }
}
