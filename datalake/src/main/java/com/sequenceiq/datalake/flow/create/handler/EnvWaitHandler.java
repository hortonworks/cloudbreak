package com.sequenceiq.datalake.flow.create.handler;

import static com.sequenceiq.datalake.service.sdx.EnvironmentService.DURATION_IN_MINUTES_FOR_ENV_POLLING;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.flow.create.event.EnvWaitRequest;
import com.sequenceiq.datalake.flow.create.event.EnvWaitSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class EnvWaitHandler extends ExceptionCatcherEventHandler<EnvWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvWaitHandler.class);

    @Inject
    private EnvironmentService environmentService;

    @Override
    public String selector() {
        return "EnvWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new SdxCreateFailedEvent(resourceId, null, null, e);
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        EnvWaitRequest envWaitRequest = event.getData();
        Long sdxId = envWaitRequest.getResourceId();
        String userId = envWaitRequest.getUserId();
        String requestId = envWaitRequest.getRequestId();
        MDCBuilder.addRequestId(requestId);
        Selectable response;
        try {
            LOGGER.debug("start polling env for sdx: {}", sdxId);
            DetailedEnvironmentResponse detailedEnvironmentResponse = environmentService.waitAndGetEnvironment(sdxId, requestId);
            response = new EnvWaitSuccessEvent(sdxId, userId, requestId, detailedEnvironmentResponse);
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Env polling exited before timeout. Cause: ", userBreakException);
            response = new SdxCreateFailedEvent(sdxId, userId, requestId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Env poller stopped for sdx: {}", sdxId, pollerStoppedException);
            response = new SdxCreateFailedEvent(sdxId, userId, requestId,
                    new PollerStoppedException("Env wait timed out after " + DURATION_IN_MINUTES_FOR_ENV_POLLING + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Env polling failed for sdx: {}", sdxId, exception);
            response = new SdxCreateFailedEvent(sdxId, userId, requestId, exception);
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in sdx creation wait phase", anotherException);
            response = new SdxCreateFailedEvent(sdxId, userId, requestId, anotherException);
        }
        sendEvent(response, event);
    }
}
