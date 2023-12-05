package com.sequenceiq.freeipa.flow.freeipa.rebuild.handler;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup.FreeIpaCleanupAfterRestoreFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup.FreeIpaCleanupAfterRestoreRequest;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup.FreeIpaCleanupAfterRestoreSuccess;

@Component
public class FreeIpaCleanupAfterRestoreHandler extends ExceptionCatcherEventHandler<FreeIpaCleanupAfterRestoreRequest> {
    @Override
    public String selector() {
        return EventSelectorUtil.selector(FreeIpaCleanupAfterRestoreRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FreeIpaCleanupAfterRestoreRequest> event) {
        return new FreeIpaCleanupAfterRestoreFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FreeIpaCleanupAfterRestoreRequest> event) {
        return new FreeIpaCleanupAfterRestoreSuccess(event.getData().getResourceId());
    }
}
