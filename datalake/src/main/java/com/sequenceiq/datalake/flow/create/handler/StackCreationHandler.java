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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.flow.create.event.StackCreationSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.StackCreationWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.ProvisionerService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

@Component
public class StackCreationHandler extends ExceptionCatcherEventHandler<StackCreationWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreationHandler.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxService sdxService;

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
        return new SdxCreateFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent handlerEvent) {
        StackCreationWaitRequest stackCreationWaitRequest = handlerEvent.getData();
        Long sdxId = stackCreationWaitRequest.getResourceId();
        String userId = stackCreationWaitRequest.getUserId();
        Selectable response;
        try {
            LOGGER.debug("start polling stack creation process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            StackV4Response stackV4Response = provisionerService.waitCloudbreakClusterCreation(sdxId, pollingConfig);
            SdxCluster sdxCluster = sdxService.getById(sdxId);
            sdxService.updateRuntimeVersionFromStackResponse(sdxCluster, stackV4Response);
            setStackCreatedStatus(sdxId);
            response = new StackCreationSuccessEvent(sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Polling exited before timeout for SDX: {}. Cause: ", sdxId, userBreakException);
            response = new SdxCreateFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Poller stopped for SDX: {}", sdxId, pollerStoppedException);
            response = new SdxCreateFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake stack creation timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Polling failed for stack: {}", sdxId, exception);
            response = new SdxCreateFailedEvent(sdxId, userId, exception);
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in stack creation wait phase", anotherException);
            response = new SdxCreateFailedEvent(sdxId, userId, anotherException);
        }
        return response;
    }

    private void setStackCreatedStatus(Long datalakeId) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_CREATION_FINISHED, "Datalake stack created", datalakeId);
    }

}
