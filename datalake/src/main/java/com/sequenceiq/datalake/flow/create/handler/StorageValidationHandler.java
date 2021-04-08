package com.sequenceiq.datalake.flow.create.handler;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
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

import reactor.bus.Event;

@Component
public class StorageValidationHandler extends ExceptionCatcherEventHandler<StorageValidationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageValidationHandler.class);

    @Value("${sdx.db.operation.duration_min:60}")
    private int durationInMinutes;

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
            DetailedEnvironmentResponse env = environmentService.waitNetworkAndGetEnvironment(sdxId);
            Optional<SdxCluster> sdxCluster = sdxClusterRepository.findById(sdxId);
            if (sdxCluster.isPresent()) {
                stackRequestManifester.configureStackForSdxCluster(sdxCluster.get(), env);
                return new StorageValidationSuccessEvent(sdxId, userId);
            } else {
                throw notFound("SDX cluster", sdxId).get();
            }
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Env polling exited before timeout. Cause: ", userBreakException);
            return new SdxCreateFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Env poller stopped for sdx: {}", sdxId, pollerStoppedException);
            return new SdxCreateFailedEvent(sdxId, userId,
                    new PollerStoppedException("Env wait timed out after " + durationInMinutes + " minutes in sdx storage validation phase"));
        } catch (PollerException exception) {
            LOGGER.error("Env polling failed for sdx: {}", sdxId, exception);
            return new SdxCreateFailedEvent(sdxId, userId, exception);
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in sdx storage validation phase", anotherException);
            return new SdxCreateFailedEvent(sdxId, userId, anotherException);
        }
    }

}
