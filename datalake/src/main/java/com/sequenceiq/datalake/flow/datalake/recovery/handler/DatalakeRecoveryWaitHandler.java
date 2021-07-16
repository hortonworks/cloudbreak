package com.sequenceiq.datalake.flow.datalake.recovery.handler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.flow.datalake.recovery.event.DatalakeRecoveryFailedEvent;
import com.sequenceiq.datalake.flow.datalake.recovery.event.DatalakeRecoverySuccessEvent;
import com.sequenceiq.datalake.flow.datalake.recovery.event.DatalakeRecoveryWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxRecoveryService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class DatalakeRecoveryWaitHandler extends ExceptionCatcherEventHandler<DatalakeRecoveryWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeRecoveryWaitHandler.class);

    private static final int SLEEP_TIME_IN_SEC = 20;

    private static final int DURATION_IN_MINUTES = 90;

    @Inject
    private SdxRecoveryService recoveryService;

    @Override
    public String selector() {
        return "DatalakeRecoveryWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeRecoveryWaitRequest> event) {
        return new DatalakeRecoveryFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeRecoveryWaitRequest> event) {
        DatalakeRecoveryWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        Selectable response;
        try {
            LOGGER.info("Start polling cluster recovery process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
            recoveryService.waitCloudbreakFlow(sdxId, pollingConfig, "Stack Recovery");
            response = new DatalakeRecoverySuccessEvent(sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Recovery polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeRecoveryFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Recovery poller stopped for cluster: {}", sdxId);
            response = new DatalakeRecoveryFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake recovery timed out after " + DURATION_IN_MINUTES + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Recovery polling failed for cluster: {}", sdxId);
            response = new DatalakeRecoveryFailedEvent(sdxId, userId, exception);
        }
        return response;
    }
}
