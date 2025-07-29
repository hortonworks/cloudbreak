package com.sequenceiq.freeipa.flow.freeipa.trust.cancel.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.CancelTrustSetupConfigurationFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.CancelTrustSetupConfigurationRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.CancelTrustSetupConfigurationSuccess;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.CancelTrustService;

@Component
public class CancelTrustSetupConfigurationHandler extends ExceptionCatcherEventHandler<CancelTrustSetupConfigurationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelTrustSetupConfigurationHandler.class);

    @Inject
    private CancelTrustService cancelTrustService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CancelTrustSetupConfigurationRequest> event) {
        LOGGER.error("Failed to cancel trust on FreeIPA", e);
        return new CancelTrustSetupConfigurationFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CancelTrustSetupConfigurationRequest> event) {
        CancelTrustSetupConfigurationRequest request = event.getData();
        try {
            cancelTrustService.cancelTrust(request.getResourceId());
            return new CancelTrustSetupConfigurationSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Failed to cancel trust on FreeIPA", e);
            return new CancelTrustSetupConfigurationFailed(request.getResourceId(), e);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CancelTrustSetupConfigurationRequest.class);
    }
}
