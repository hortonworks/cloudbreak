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
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupPrepareServerFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupPrepareServerRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupPrepareServerSuccess;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.TrustProvider;

@Component
public class FreeIpaTrustSetupPrepareServerHandler extends ExceptionCatcherEventHandler<FreeIpaTrustSetupPrepareServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaTrustSetupPrepareServerHandler.class);

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FreeIpaTrustSetupPrepareServerRequest> event) {
        return new FreeIpaTrustSetupPrepareServerFailed(resourceId, e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FreeIpaTrustSetupPrepareServerRequest> event) {
        FreeIpaTrustSetupPrepareServerRequest request = event.getData();
        try {
            TrustProvider trustProvider = crossRealmTrustService.getTrustProvider(request.getResourceId());
            trustProvider.prepare(request.getResourceId());
            return new FreeIpaTrustSetupPrepareServerSuccess(request.getResourceId());
        } catch (Exception e) {
            return new FreeIpaTrustSetupPrepareServerFailed(request.getResourceId(), e, ERROR);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FreeIpaTrustSetupPrepareServerRequest.class);
    }
}
