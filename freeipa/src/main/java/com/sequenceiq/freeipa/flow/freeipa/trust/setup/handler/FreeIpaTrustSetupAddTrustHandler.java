package com.sequenceiq.freeipa.flow.freeipa.trust.setup.handler;

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
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupAddTrustFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupAddTrustRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupAddTrustSuccess;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.TrustProvider;

@Component
public class FreeIpaTrustSetupAddTrustHandler extends ExceptionCatcherEventHandler<FreeIpaTrustSetupAddTrustRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaTrustSetupAddTrustHandler.class);

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FreeIpaTrustSetupAddTrustRequest> event) {
        LOGGER.error("Failed to add trust on FreeIPA", e);
        return new FreeIpaTrustSetupAddTrustFailed(resourceId, e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FreeIpaTrustSetupAddTrustRequest> event) {
        FreeIpaTrustSetupAddTrustRequest request = event.getData();
        try {
            TrustProvider trustProvider = crossRealmTrustService.getTrustProvider(request.getResourceId());
            trustProvider.addTrust(request.getResourceId());
            return new FreeIpaTrustSetupAddTrustSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Failed to add trust on FreeIPA", e);
            return new FreeIpaTrustSetupAddTrustFailed(request.getResourceId(), e, ERROR);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FreeIpaTrustSetupAddTrustRequest.class);
    }
}

