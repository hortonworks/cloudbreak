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
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.PrepareIpaServerFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.PrepareIpaServerRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.PrepareIpaServerSuccess;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.PrepareIpaServerService;

@Component
public class PrepareIpaServerHandler extends ExceptionCatcherEventHandler<PrepareIpaServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareIpaServerHandler.class);

    @Inject
    private PrepareIpaServerService prepareIpaServerService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<PrepareIpaServerRequest> event) {
        return new PrepareIpaServerFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<PrepareIpaServerRequest> event) {
        PrepareIpaServerRequest request = event.getData();
        try {
            prepareIpaServerService.prepareIpaServer(request.getResourceId());
            return new PrepareIpaServerSuccess(request.getResourceId());
        } catch (Exception e) {
            return new PrepareIpaServerFailed(request.getResourceId(), e);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(PrepareIpaServerRequest.class);
    }
}
