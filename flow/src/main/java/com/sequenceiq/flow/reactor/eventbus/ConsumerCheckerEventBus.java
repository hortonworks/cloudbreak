package com.sequenceiq.flow.reactor.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.reactor.handler.ConsumerNotFoundException;

import reactor.bus.Event;
import reactor.bus.EventBus;

public class ConsumerCheckerEventBus extends EventBus {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerCheckerEventBus.class);

    public ConsumerCheckerEventBus(EventBus eventBus) {
        super(eventBus.getConsumerRegistry(), eventBus.getDispatcher(), eventBus.getRouter(), eventBus.getDispatchErrorHandler(),
                eventBus.getUncaughtErrorHandler());
    }

    @Override
    public void accept(Event<?> event) {
        try {
            getConsumerRegistry().select(event.getKey());
        } catch (ConsumerNotFoundException e) {
            LOGGER.error("Could not find consumer for event: " + e.getEvent(), e);
            throw new EventCanNotBeDeliveredException(event);
        }
        super.accept(event);
    }

}
