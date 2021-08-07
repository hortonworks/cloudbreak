package com.sequenceiq.datalake.flow.datalake.recovery.handler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class DatalakeRecoveryWaitHandler extends ExceptionCatcherEventHandler<DatalakeRecoveryWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeRecoveryWaitHandler.class);

    @Value("${sdx.stack.recovery.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.recovery.duration_min:90}")
    private int durationInMinutes;

    @Inject
    private SdxRecoveryService recoveryService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeRecoveryWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeRecoveryWaitRequest> event) {
        return new DatalakeRecoveryFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeRecoveryWaitRequest> event) {
        DatalakeRecoveryWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        Selectable response;
        try {
            LOGGER.info("Start polling cluster recovery process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            recoveryService.waitCloudbreakFlow(sdxId, pollingConfig, "Stack Recovery");
            response = new DatalakeRecoverySuccessEvent(sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Recovery polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeRecoveryFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Recovery poller stopped for cluster: {}", sdxId);
            response = new DatalakeRecoveryFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake recovery timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Recovery polling failed for cluster: {}", sdxId);
            response = new DatalakeRecoveryFailedEvent(sdxId, userId, exception);
        }
        return response;
    }
}
