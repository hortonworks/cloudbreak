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
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.flow.create.event.SdxValidationRequest;
import com.sequenceiq.datalake.flow.create.event.SdxValidationSuccessEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.datalake.service.sdx.SdxRecommendationService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SdxValidationHandler extends ExceptionCatcherEventHandler<SdxValidationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxValidationHandler.class);

    @Value("${sdx.db.operation.duration_min:60}")
    private int durationInMinutes;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxRecommendationService sdxRecommendationService;

    @Inject
    private EnvironmentService environmentService;

    @Override
    public String selector() {
        return "SdxValidationRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SdxValidationRequest> event) {
        return new SdxCreateFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SdxValidationRequest> event) {
        SdxValidationRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        try {
            DetailedEnvironmentResponse environment = environmentService.waitNetworkAndGetEnvironment(sdxId);
            Optional<SdxCluster> sdxCluster = sdxClusterRepository.findById(sdxId);
            if (sdxCluster.isPresent()) {
                sdxRecommendationService.validateVmTypeOverride(environment, sdxCluster.get());
                sdxRecommendationService.validateRecommendedImage(environment, sdxCluster.get());
                return new SdxValidationSuccessEvent(sdxId, userId);
            } else {
                throw notFound("SDX cluster", sdxId).get();
            }
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Env polling exited before timeout. Cause: ", userBreakException);
            return new SdxCreateFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Env poller stopped for sdx: {}", sdxId, pollerStoppedException);
            return new SdxCreateFailedEvent(sdxId, userId,
                    new PollerStoppedException("Env wait timed out after " + durationInMinutes + " minutes in sdx validation phase"));
        } catch (PollerException exception) {
            LOGGER.error("Env polling failed for sdx: {}", sdxId, exception);
            return new SdxCreateFailedEvent(sdxId, userId, exception);
        } catch (Exception e) {
            LOGGER.error("Sdx validation failed", e);
            return new SdxCreateFailedEvent(sdxId, userId, e);
        }
    }
}
