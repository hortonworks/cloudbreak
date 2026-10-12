package com.sequenceiq.datalake.flow.create.handler;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.create.event.RdsWaitRequest;
import com.sequenceiq.datalake.flow.create.event.RdsWaitSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RdsWaitHandler extends ExceptionCatcherEventHandler<RdsWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsWaitHandler.class);

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private DatabaseService databaseService;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private RdsCreationHandlerSupport rdsCreationHandlerSupport;

    @Override
    public String selector() {
        return "RdsWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RdsWaitRequest> event) {
        return new SdxCreateFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RdsWaitRequest> event) {
        RdsWaitRequest rdsWaitRequest = event.getData();
        Long sdxId = rdsWaitRequest.getResourceId();
        String userId = rdsWaitRequest.getUserId();
        try {
            DetailedEnvironmentResponse environment = environmentService.waitNetworkAndGetEnvironment(sdxId);
            Optional<SdxCluster> sdxCluster = sdxClusterRepository.findById(sdxId);
            if (sdxCluster.isPresent()) {
                if (sdxCluster.get().hasExternalDatabase()) {
                    rdsCreationHandlerSupport.validForDatabaseCreation(sdxId, environment);
                    LOGGER.debug("start polling database for Data Lake: {}", sdxId);
                    databaseService.createAndWaitUntilAvailable(sdxCluster.get(), environment);
                    rdsCreationHandlerSupport.setRdsCreatedStatus(sdxCluster.get());
                    return new RdsWaitSuccessEvent(sdxId, userId);
                } else {
                    LOGGER.debug("skipping creation of database for Data Lake: {}", sdxId);
                    return new RdsWaitSuccessEvent(sdxId, userId);
                }
            } else {
                throw notFound("Data Lake cluster", sdxId).get();
            }
        } catch (Exception exception) {
            return rdsCreationHandlerSupport.toFailedEvent(sdxId, userId, exception);
        }
    }
}
