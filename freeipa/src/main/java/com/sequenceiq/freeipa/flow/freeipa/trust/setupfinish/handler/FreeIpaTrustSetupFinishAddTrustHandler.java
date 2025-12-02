package com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishAddRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishAddTrustFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishAddTrustSuccess;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.TrustProvider;

@Component
public class FreeIpaTrustSetupFinishAddTrustHandler extends ExceptionCatcherEventHandler<FreeIpaTrustSetupFinishAddRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaTrustSetupFinishAddTrustHandler.class);

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FreeIpaTrustSetupFinishAddRequest> event) {
        LOGGER.error("Failed to add trust on FreeIPA", e);
        return new FreeIpaTrustSetupFinishAddTrustFailed(resourceId, e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FreeIpaTrustSetupFinishAddRequest> event) {
        FreeIpaTrustSetupFinishAddRequest request = event.getData();
        try {
            TrustProvider trustProvider = crossRealmTrustService.getTrustProvider(request.getResourceId());
            trustProvider.addTrust(request.getResourceId());
            trustProvider.validateTrust(request.getResourceId());
            return new FreeIpaTrustSetupFinishAddTrustSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Failed to add trust on FreeIPA", e);
            return new FreeIpaTrustSetupFinishAddTrustFailed(request.getResourceId(), e, ERROR);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FreeIpaTrustSetupFinishAddRequest.class);
    }
}
