package com.sequenceiq.datalake.flow.repair.handler;

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
import com.sequenceiq.datalake.flow.repair.event.SdxRepairFailedEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairSuccessEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxRepairService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class SdxRepairWaitHandler extends ExceptionCatcherEventHandler<SdxRepairWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRepairWaitHandler.class);

    @Value("${sdx.stack.repair.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.repair.duration_min:60}")
    private int durationInMinutes;

    @Inject
    private SdxRepairService repairService;

    @Override
    public String selector() {
        return "SdxRepairWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SdxRepairWaitRequest> event) {
        return new SdxRepairFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SdxRepairWaitRequest> event) {
        SdxRepairWaitRequest sdxRepairWaitRequest = event.getData();
        Long sdxId = sdxRepairWaitRequest.getResourceId();
        String userId = sdxRepairWaitRequest.getUserId();
        Selectable response;
        try {
            LOGGER.debug("Start polling stack repair process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            repairService.waitCloudbreakClusterRepair(sdxId, pollingConfig);
            response = new SdxRepairSuccessEvent(sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Repair polling exited before timeout. Cause: ", userBreakException);
            response = new SdxRepairFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Repair poller stopped for stack: {}", sdxId);
            response = new SdxRepairFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake repair timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Repair polling failed for stack: {}", sdxId);
            response = new SdxRepairFailedEvent(sdxId, userId, exception);
        }
        return response;
    }
}
