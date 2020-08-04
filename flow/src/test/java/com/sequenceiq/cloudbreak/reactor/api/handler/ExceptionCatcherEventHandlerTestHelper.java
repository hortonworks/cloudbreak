package com.sequenceiq.cloudbreak.reactor.api.handler;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import org.springframework.stereotype.Component;

@Component
class ExceptionCatcherEventHandlerTestHelper extends ExceptionCatcherEventHandler<Payload> {

    private String selectorString;

    private Selectable failureEvent;

    private boolean throwException;

    private Selectable sendEventObject;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return failureEvent;
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        if (throwException) {
            throw new RuntimeException("Expected exception");
        }
        if (sendEventObject != null) {
            sendEvent(sendEventObject, event);
        }
    }

    @Override
    public String selector() {
        return selectorString;
    }

    public void initialize(String selectorString, Selectable failureEvent, boolean throwException,
            Selectable sendEventObject) {
        this.selectorString = selectorString;
        this.failureEvent = failureEvent;
        this.throwException = throwException;
        this.sendEventObject = sendEventObject;
    }
}
