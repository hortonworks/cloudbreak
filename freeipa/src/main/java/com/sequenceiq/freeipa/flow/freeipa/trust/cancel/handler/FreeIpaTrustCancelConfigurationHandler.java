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
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelConfigurationFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelConfigurationRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelConfigurationSuccess;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.TrustProvider;

@Component
public class FreeIpaTrustCancelConfigurationHandler extends ExceptionCatcherEventHandler<FreeIpaTrustCancelConfigurationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaTrustCancelConfigurationHandler.class);

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FreeIpaTrustCancelConfigurationRequest> event) {
        LOGGER.error("Failed to cancel trust on FreeIPA", e);
        return new FreeIpaTrustCancelConfigurationFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FreeIpaTrustCancelConfigurationRequest> event) {
        FreeIpaTrustCancelConfigurationRequest request = event.getData();
        try {
            TrustProvider trustProvider = crossRealmTrustService.getTrustProvider(request.getResourceId());
            trustProvider.deleteTrust(request.getResourceId());
            trustProvider.deleteDnsZones(request.getResourceId());
            return new FreeIpaTrustCancelConfigurationSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Failed to cancel trust on FreeIPA", e);
            return new FreeIpaTrustCancelConfigurationFailed(request.getResourceId(), e);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FreeIpaTrustCancelConfigurationRequest.class);
    }
}
