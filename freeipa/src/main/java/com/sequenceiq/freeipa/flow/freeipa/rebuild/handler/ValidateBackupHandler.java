package com.sequenceiq.freeipa.flow.freeipa.rebuild.handler;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupRequest;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupSuccess;

@Component
public class ValidateBackupHandler extends ExceptionCatcherEventHandler<ValidateBackupRequest> {
    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateBackupRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ValidateBackupRequest> event) {
        return new ValidateBackupFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ValidateBackupRequest> event) {
        return new ValidateBackupSuccess(event.getData().getResourceId());
    }
}
