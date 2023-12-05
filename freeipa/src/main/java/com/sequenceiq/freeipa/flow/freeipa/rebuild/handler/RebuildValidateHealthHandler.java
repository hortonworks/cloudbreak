package com.sequenceiq.freeipa.flow.freeipa.rebuild.handler;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health.RebuildValidateHealthFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health.RebuildValidateHealthRequest;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health.RebuildValidateHealthSuccess;

@Component
public class RebuildValidateHealthHandler extends ExceptionCatcherEventHandler<RebuildValidateHealthRequest> {
    @Override
    public String selector() {
        return EventSelectorUtil.selector(RebuildValidateHealthRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RebuildValidateHealthRequest> event) {
        return new RebuildValidateHealthFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RebuildValidateHealthRequest> event) {
        return new RebuildValidateHealthSuccess(event.getData().getResourceId());
    }
}
