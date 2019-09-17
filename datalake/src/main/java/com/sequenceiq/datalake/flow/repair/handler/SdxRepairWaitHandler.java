package com.sequenceiq.datalake.flow.repair.handler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairFailedEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairSuccessEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxRepairService;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class SdxRepairWaitHandler implements EventHandler<SdxRepairWaitRequest> {

    public static final int SLEEP_TIME_IN_SEC = 20;

    public static final int DURATION_IN_MINUTES = 40;

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRepairWaitHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private SdxRepairService repairService;

    @Override
    public String selector() {
        return "SdxRepairWaitRequest";
    }

    @Override
    public void accept(Event<SdxRepairWaitRequest> event) {
        SdxRepairWaitRequest sdxRepairWaitRequest = event.getData();
        Long sdxId = sdxRepairWaitRequest.getResourceId();
        String userId = sdxRepairWaitRequest.getUserId();
        String requestId = sdxRepairWaitRequest.getRequestId();
        MDCBuilder.addRequestId(requestId);
        Selectable response;
        try {
            LOGGER.debug("Start polling stack deletion process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
            repairService.waitCloudbreakClusterRepair(sdxId, pollingConfig);
            response = new SdxRepairSuccessEvent(sdxId, userId, requestId);
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Repair polling exited before timeout. Cause: ", userBreakException);
            response = new SdxRepairFailedEvent(sdxId, userId, requestId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Repair poller stopped for stack: {}", sdxId);
            response = new SdxRepairFailedEvent(sdxId, userId, requestId,
                    new PollerStoppedException("Datalake repair timed out after " + DURATION_IN_MINUTES + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Repair polling failed for stack: {}", sdxId);
            response = new SdxRepairFailedEvent(sdxId, userId, requestId, exception);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
