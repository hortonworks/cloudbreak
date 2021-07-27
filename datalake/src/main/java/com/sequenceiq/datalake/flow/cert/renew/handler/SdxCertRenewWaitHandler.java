package com.sequenceiq.datalake.flow.cert.renew.handler;

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
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.cert.renew.event.SdxCertRenewalEvent;
import com.sequenceiq.datalake.flow.cert.renew.event.SdxCertRenewalFailedEvent;
import com.sequenceiq.datalake.flow.cert.renew.event.SdxCertRenewalWaitEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairFailedEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.cert.CertRenewalService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class SdxCertRenewWaitHandler extends ExceptionCatcherEventHandler<SdxCertRenewalWaitEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCertRenewWaitHandler.class);

    @Value("${sdx.stack.cert.renewal.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.cert.renewal.duration_min:60}")
    private int durationInMinutes;

    @Inject
    private CertRenewalService certRenewalService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SdxCertRenewalWaitEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SdxCertRenewalWaitEvent> event) {
        return new SdxRepairFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SdxCertRenewalWaitEvent> event) {
        SdxEvent sdxEvent = event.getData();
        Long sdxId = sdxEvent.getResourceId();
        String userId = sdxEvent.getUserId();
        Selectable response;
        try {
            LOGGER.debug("Start polling stack cert renewal process.");
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            certRenewalService.waitForCloudbreakClusterCertRenewal(sdxId, pollingConfig);
            response = new SdxEvent(SdxCertRenewalEvent.CERT_RENEWAL_FINISHED_EVENT.event(), sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Cert renewal polling exited before timeout. Cause: ", userBreakException);
            response = new SdxCertRenewalFailedEvent(sdxId, userId, userBreakException.getMessage());
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Cert renewal poller stopped.");
            response = new SdxCertRenewalFailedEvent(sdxId, userId, "Datalake cert renewal timed out after " + durationInMinutes + " minutes");
        } catch (PollerException exception) {
            LOGGER.error("Cert renewal polling failed. Cause: ", exception);
            response = new SdxCertRenewalFailedEvent(sdxId, userId, exception.getMessage());
        }
        return response;
    }
}
