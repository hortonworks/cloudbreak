package com.sequenceiq.flow.reactor.handler;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerNotFoundHandler implements Consumer<Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerNotFoundHandler.class);

    @Override
    public void accept(Object event) {
        LOGGER.error("Event not delivered! There is no registered consumer for the key: [ \"{}\" ]", event);
        throw new ConsumerNotFoundException(event);
    }
}
