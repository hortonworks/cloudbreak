package com.sequenceiq.datalake.flow.delete.handler;

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
import com.sequenceiq.datalake.flow.delete.event.SdxDeletionFailedEvent;
import com.sequenceiq.datalake.flow.delete.event.StackDeletionSuccessEvent;
import com.sequenceiq.datalake.flow.delete.event.StackDeletionWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.ProvisionerService;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class StackDeletionHandler implements EventHandler<StackDeletionWaitRequest> {

    public static final int SLEEP_TIME_IN_SEC = 10;

    public static final int DURATION_IN_MINUTES = 20;

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDeletionHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ProvisionerService provisionerService;

    @Override
    public String selector() {
        return "StackDeletionWaitRequest";
    }

    @Override
    public void accept(Event<StackDeletionWaitRequest> event) {
        StackDeletionWaitRequest stackDeletionWaitRequest = event.getData();
        Long sdxId = stackDeletionWaitRequest.getResourceId();
        String userId = stackDeletionWaitRequest.getUserId();
        String requestId = stackDeletionWaitRequest.getRequestId();
        MDCBuilder.addRequestId(requestId);
        Selectable response;
        try {
            LOGGER.debug("Start polling stack deletion process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
            provisionerService.waitCloudbreakClusterDeletion(sdxId, pollingConfig, requestId);
            response = new StackDeletionSuccessEvent(sdxId, userId, requestId, stackDeletionWaitRequest.isForced());
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Deletion polling exited before timeout. Cause: ", userBreakException);
            response = new SdxDeletionFailedEvent(sdxId, userId, requestId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Deletion poller stopped for stack: {}", sdxId);
            response = new SdxDeletionFailedEvent(sdxId, userId, requestId, pollerStoppedException);
        } catch (PollerException exception) {
            LOGGER.info("Deletion polling failed for stack: {}", sdxId);
            response = new SdxDeletionFailedEvent(sdxId, userId, requestId, exception);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
