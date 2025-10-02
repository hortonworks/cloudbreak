package com.sequenceiq.freeipa.flow.freeipa.trust.setup.handler;

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
import com.sequenceiq.freeipa.service.freeipa.trust.setup.PrepareIpaServerService;

@Component
public class FreeIpaTrustSetupPrepareServerHandler extends ExceptionCatcherEventHandler<FreeIpaTrustSetupPrepareServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaTrustSetupPrepareServerHandler.class);

    @Inject
    private PrepareIpaServerService prepareIpaServerService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FreeIpaTrustSetupPrepareServerRequest> event) {
        return new FreeIpaTrustSetupPrepareServerFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FreeIpaTrustSetupPrepareServerRequest> event) {
        FreeIpaTrustSetupPrepareServerRequest request = event.getData();
        try {
            prepareIpaServerService.prepareIpaServer(request.getResourceId());
            return new FreeIpaTrustSetupPrepareServerSuccess(request.getResourceId());
        } catch (Exception e) {
            return new FreeIpaTrustSetupPrepareServerFailed(request.getResourceId(), e);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FreeIpaTrustSetupPrepareServerRequest.class);
    }
}
