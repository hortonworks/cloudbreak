package com.sequenceiq.datalake.flow.delete.handler;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDeletionHandler.class);

    @Value("${sdx.stack.deletion.sleeptime_sec:10}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.deletion.duration_min:60}")
    private int durationInMinutes;

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
        Selectable response;
        try {
            LOGGER.debug("Start polling stack deletion process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            provisionerService.waitCloudbreakClusterDeletion(sdxId, pollingConfig);
            response = new StackDeletionSuccessEvent(sdxId, userId, stackDeletionWaitRequest.isForced());
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Deletion polling exited before timeout. Cause: ", userBreakException);
            response = new SdxDeletionFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Deletion poller stopped for stack: {}", sdxId);
            response = new SdxDeletionFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake stack deletion timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Deletion polling failed for stack: {}", sdxId);
            response = new SdxDeletionFailedEvent(sdxId, userId, exception);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
