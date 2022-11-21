package com.sequenceiq.datalake.flow.create.handler;

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
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.create.event.EnvWaitRequest;
import com.sequenceiq.datalake.flow.create.event.EnvWaitSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class EnvWaitHandler extends ExceptionCatcherEventHandler<EnvWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvWaitHandler.class);

    @Value("${sdx.environment.duration_min:60}")
    private int durationInMinutes;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Override
    public String selector() {
        return "EnvWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvWaitRequest> event) {
        return new SdxCreateFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvWaitRequest> event) {
        EnvWaitRequest envWaitRequest = event.getData();
        Long datalakeId = envWaitRequest.getResourceId();
        String userId = envWaitRequest.getUserId();
        Selectable response;
        try {
            LOGGER.debug("start polling env for sdx: {}", datalakeId);
            DetailedEnvironmentResponse detailedEnvironmentResponse = environmentService.waitAndGetEnvironment(datalakeId);

            response = new EnvWaitSuccessEvent(datalakeId, userId, detailedEnvironmentResponse);
            setEnvCreatedStatus(datalakeId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Env polling exited before timeout. Cause: ", userBreakException);
            response = new SdxCreateFailedEvent(datalakeId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Env poller stopped for SDX: {}", datalakeId, pollerStoppedException);
            response = new SdxCreateFailedEvent(datalakeId, userId,
                    new PollerStoppedException("Env wait timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Env polling failed for SDX: {}", datalakeId, exception);
            response = new SdxCreateFailedEvent(datalakeId, userId, exception);
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in SDX creation wait phase", anotherException);
            response = new SdxCreateFailedEvent(datalakeId, userId, anotherException);
        }
        return response;
    }

    private void setEnvCreatedStatus(Long datalakeId) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.ENVIRONMENT_CREATED, "Environment created", datalakeId);
    }

}
