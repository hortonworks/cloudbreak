package com.sequenceiq.datalake.flow.create.handler;

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
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.flow.create.event.StackCreationSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.StackCreationWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.ProvisionerService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Component
public class StackCreationHandler extends ExceptionCatcherEventHandler<StackCreationWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreationHandler.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Value("${sdx.stack.provision.sleeptime_sec:10}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.provision.duration_min:120}")
    private int durationInMinutes;

    @Inject
    private ProvisionerService provisionerService;

    @Override
    public String selector() {
        return "StackCreationWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new SdxCreateFailedEvent(resourceId, null, null, e);
    }

    @Override
    protected void doAccept(HandlerEvent handlerEvent) {
        StackCreationWaitRequest stackCreationWaitRequest = handlerEvent.getData();
        Long sdxId = stackCreationWaitRequest.getResourceId();
        String userId = stackCreationWaitRequest.getUserId();
        String requestId = stackCreationWaitRequest.getRequestId();
        MDCBuilder.addRequestId(requestId);
        Selectable response;
        try {
            LOGGER.debug("start polling stack creation process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            provisionerService.waitCloudbreakClusterCreation(sdxId, pollingConfig, requestId);
            setStackCreatedStatus(sdxId);
            response = new StackCreationSuccessEvent(sdxId, userId, requestId);
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Polling exited before timeout for SDX: {}. Cause: ", sdxId, userBreakException);
            response = new SdxCreateFailedEvent(sdxId, userId, requestId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Poller stopped for SDX: {}", sdxId, pollerStoppedException);
            response = new SdxCreateFailedEvent(sdxId, userId, requestId,
                    new PollerStoppedException("Datalake stack creation timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Polling failed for stack: {}", sdxId, exception);
            response = new SdxCreateFailedEvent(sdxId, userId, requestId, exception);
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in stack creation wait phase", anotherException);
            response = new SdxCreateFailedEvent(sdxId, userId, requestId, anotherException);
        }
        sendEvent(response, handlerEvent);
    }

    private void setStackCreatedStatus(Long datalakeId) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_CREATION_FINISHED,
                ResourceEvent.SDX_CLUSTER_PROVISION_FINISHED, "Datalake stack created", datalakeId);
    }
}
