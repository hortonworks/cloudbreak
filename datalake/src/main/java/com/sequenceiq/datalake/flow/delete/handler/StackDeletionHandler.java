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
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class StackDeletionHandler extends ExceptionCatcherEventHandler<StackDeletionWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDeletionHandler.class);

    @Value("${sdx.stack.deletion.sleeptime_sec:10}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.deletion.duration_min:60}")
    private int durationInMinutes;

    @Inject
    private ProvisionerService provisionerService;

    @Override
    public String selector() {
        return "StackDeletionWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<StackDeletionWaitRequest> event) {
        return new SdxDeletionFailedEvent(resourceId, null, e, event.getData().isForced());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<StackDeletionWaitRequest> event) {
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
            LOGGER.error("Deletion polling exited before timeout. Cause: ", userBreakException);
            response = new SdxDeletionFailedEvent(sdxId, userId, userBreakException, stackDeletionWaitRequest.isForced());
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Deletion poller stopped for stack: {}", sdxId);
            response = new SdxDeletionFailedEvent(
                    sdxId,
                    userId,
                    new PollerStoppedException("Datalake stack deletion timed out after " + durationInMinutes + " minutes"),
                    stackDeletionWaitRequest.isForced());
        } catch (PollerException exception) {
            LOGGER.error("Deletion polling failed for stack: {}", sdxId);
            response = new SdxDeletionFailedEvent(sdxId, userId, exception, stackDeletionWaitRequest.isForced());
        }
        return response;
    }
}
