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

    protected abstract Selectable defaultFailureEvent(Long resourceId, Exception e, Event<T> event);

    protected abstract Selectable doAccept(HandlerEvent event);

    @Override
    public void accept(Event<T> event) {
        String handlerName = getClass().getSimpleName();
        try {
            HandlerEvent handlerEvent = new HandlerEvent(event);
            sendEvent(doAccept(handlerEvent), handlerEvent);
            if (handlerEvent.getCounter() < 1) {
                String message = "No event has been sent from " + handlerName;
                LOGGER.error(message);
                throw new IllegalStateException(message);
            }
        } catch (Exception e) {
            LOGGER.error("Something unexpected happened in handler {}", handlerName, e);
            Selectable failureEvent = defaultFailureEvent(event.getData().getResourceId(), e, event);
            eventBus.notify(failureEvent.selector(), new Event<>(event.getHeaders(), failureEvent));
        }
    }

    private void sendEvent(Selectable event, HandlerEvent originalEvent) {
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
