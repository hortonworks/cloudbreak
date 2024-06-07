package com.sequenceiq.datalake.flow.certrotation.handler;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.certrotation.event.RotateCertificateFailedEvent;
import com.sequenceiq.datalake.flow.certrotation.event.RotateCertificateStackRequest;
import com.sequenceiq.datalake.flow.certrotation.event.RotateCertificateSuccessEvent;
import com.sequenceiq.datalake.service.rotation.certificate.SdxDatabaseCertificateRotationService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RotateCertificateStackHandler extends ExceptionCatcherEventHandler<RotateCertificateStackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateCertificateStackHandler.class);

    @Value("${sdx.rotate.certificate.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.rotate.certificate.duration_min:20}")
    private int durationInMinutes;

    @Inject
    private SdxDatabaseCertificateRotationService certificateRotationService;

    @Inject
    private SdxService sdxService;

    @Override
    public String selector() {
        return RotateCertificateStackRequest.class.getSimpleName();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RotateCertificateStackRequest> event) {
        return new RotateCertificateFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RotateCertificateStackRequest> event) {
        RotateCertificateStackRequest request = event.getData();
        SdxCluster sdxCluster = sdxService.getById(request.getResourceId());
        Long sdxId = sdxCluster.getId();
        String userId = request.getUserId();
        Selectable response;
        try {
            LOGGER.debug("Initiating Certificate Rotation and start polling for SDX: {}", sdxCluster.getName());
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            certificateRotationService.initAndWaitForStackCertificateRotation(sdxCluster, pollingConfig);
            response = new RotateCertificateSuccessEvent(sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Rotate Certificate poller exited before timeout. Cause: ", userBreakException);
            response = new RotateCertificateFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Rotate Certificate poller stopped for stack: {}", sdxId);
            response = new RotateCertificateFailedEvent(sdxId, userId,
                    new PollerStoppedException("Rotate Certificate timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Rotate Certificate polling failed for stack: {}", sdxId);
            response = new RotateCertificateFailedEvent(sdxId, userId, exception);
        }
        return response;
    }
}
