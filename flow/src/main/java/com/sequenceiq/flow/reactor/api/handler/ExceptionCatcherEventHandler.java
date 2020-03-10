package com.sequenceiq.flow.reactor.api.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public abstract class ExceptionCatcherEventHandler<T extends Payload> implements EventHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionCatcherEventHandler.class);

    @Inject
    private EventBus eventBus;

    protected abstract Selectable defaultFailureEvent(Long resourceId, Exception e);

    protected abstract void doAccept(HandlerEvent event);

    @Override
    public void accept(Event<T> event) {
        String handlerName = getClass().getSimpleName();
        try {
            HandlerEvent handlerEvent = new HandlerEvent(event);
            doAccept(handlerEvent);
            if (handlerEvent.getCounter() < 1) {
                LOGGER.error("No event has been sent from {}", handlerName);
                IllegalStateException noEventHasBeenSentException = new IllegalStateException("No event has been sent from " + handlerName);
                eventBus.notify(defaultFailureEvent(event.getData().getResourceId(), noEventHasBeenSentException).selector(),
                        new Event<>(event.getHeaders(), event));
            }
        } catch (Exception e) {
            LOGGER.error("Something unexpected happened in handler {}", handlerName, e);
            throw new RuntimeException(e);
//            eventBus.notify(defaultFailureEvent(event.getData().getResourceId(), e).selector(), new Event<>(event.getHeaders(), event));
        }
    }

    protected void sendEvent(Selectable event, HandlerEvent originalEvent) {
        eventBus.notify(event.selector(), new Event<>(originalEvent.getEvent().getHeaders(), event));
        originalEvent.increaseCounter();
    }

    protected class HandlerEvent {

        private Event<T> event;

        private int eventCounter;

        public HandlerEvent(Event<T> event) {
            this.event = event;
        }

        public Event<T> getEvent() {
            return event;
        }

        public void setEvent(Event<T> event) {
            this.event = event;
        }

        public T getData() {
            return event.getData();
        }

        private void increaseCounter() {
            eventCounter++;
        }

        private int getCounter() {
            return eventCounter;
        }
    }
}
