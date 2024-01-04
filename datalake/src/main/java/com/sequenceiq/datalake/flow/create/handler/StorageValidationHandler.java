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
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.flow.create.event.StorageValidationRequest;
import com.sequenceiq.datalake.flow.create.event.StorageValidationSuccessEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.datalake.service.sdx.StackRequestManifester;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class StorageValidationHandler extends ExceptionCatcherEventHandler<StorageValidationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageValidationHandler.class);

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private StackRequestManifester stackRequestManifester;

    @Override
    public String selector() {
        return "StorageValidationRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<StorageValidationRequest> event) {
        return new SdxCreateFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<StorageValidationRequest> event) {
        StorageValidationRequest storageValidationRequest = event.getData();
        Long sdxId = storageValidationRequest.getResourceId();
        String userId = storageValidationRequest.getUserId();
        try {
            Optional<SdxCluster> sdxCluster = sdxClusterRepository.findById(sdxId);
            if (sdxCluster.isPresent()) {
                DetailedEnvironmentResponse env = environmentService.getDetailedEnvironmentResponseByName(sdxCluster.get().getEnvName());
                stackRequestManifester.configureStackForSdxCluster(sdxCluster.get(), env);
                return new StorageValidationSuccessEvent(sdxId, userId);
            } else {
                throw notFound("SDX cluster", sdxId).get();
            }
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in sdx storage validation phase", anotherException);
            return new SdxCreateFailedEvent(sdxId, userId, anotherException);
        }
    }

}
