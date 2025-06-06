package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event.FinishCrossRealmTrustAddTrustFailed;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event.FinishCrossRealmTrustAddTrustRequest;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event.FinishCrossRealmTrustAddTrustSuccess;
import com.sequenceiq.freeipa.service.freeipa.crossrealm.CrossRealmAddTrustService;

@Component
public class AddTrustHandler extends ExceptionCatcherEventHandler<FinishCrossRealmTrustAddTrustRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddTrustHandler.class);

    @Inject
    private CrossRealmAddTrustService crossRealmAddTrustService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FinishCrossRealmTrustAddTrustRequest> event) {
        return new FinishCrossRealmTrustAddTrustFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FinishCrossRealmTrustAddTrustRequest> event) {
        FinishCrossRealmTrustAddTrustRequest request = event.getData();
        try {
            crossRealmAddTrustService.addTrust(request.getResourceId());
            return new FinishCrossRealmTrustAddTrustSuccess(request.getResourceId());
        } catch (Exception e) {
            return new FinishCrossRealmTrustAddTrustFailed(request.getResourceId(), e);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FinishCrossRealmTrustAddTrustRequest.class);
    }
}
