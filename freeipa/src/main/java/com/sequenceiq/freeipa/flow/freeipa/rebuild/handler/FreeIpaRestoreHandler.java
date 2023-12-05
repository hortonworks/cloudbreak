package com.sequenceiq.freeipa.flow.freeipa.rebuild.handler;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore.FreeIpaRestoreFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore.FreeIpaRestoreRequest;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore.FreeIpaRestoreSuccess;

@Component
public class FreeIpaRestoreHandler extends ExceptionCatcherEventHandler<FreeIpaRestoreRequest> {
    @Override
    public String selector() {
        return EventSelectorUtil.selector(FreeIpaRestoreRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FreeIpaRestoreRequest> event) {
        return new FreeIpaRestoreFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FreeIpaRestoreRequest> event) {
        return new FreeIpaRestoreSuccess(event.getData().getResourceId());
    }
}
