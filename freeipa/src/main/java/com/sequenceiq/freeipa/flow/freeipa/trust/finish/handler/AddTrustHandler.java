package com.sequenceiq.freeipa.flow.freeipa.trust.finish.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.event.FinishTrustSetupAddTrustFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.event.FinishTrustSetupAddTrustRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.event.FinishTrustSetupAddTrustSuccess;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.AddTrustService;

@Component
public class AddTrustHandler extends ExceptionCatcherEventHandler<FinishTrustSetupAddTrustRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddTrustHandler.class);

    @Inject
    private AddTrustService addTrustService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FinishTrustSetupAddTrustRequest> event) {
        LOGGER.error("Failed to add trust on FreeIPA", e);
        return new FinishTrustSetupAddTrustFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FinishTrustSetupAddTrustRequest> event) {
        FinishTrustSetupAddTrustRequest request = event.getData();
        try {
            addTrustService.addTrust(request.getResourceId());
            return new FinishTrustSetupAddTrustSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Failed to add trust on FreeIPA", e);
            return new FinishTrustSetupAddTrustFailed(request.getResourceId(), e);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FinishTrustSetupAddTrustRequest.class);
    }
}
