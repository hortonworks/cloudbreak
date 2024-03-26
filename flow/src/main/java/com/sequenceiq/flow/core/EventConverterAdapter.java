package com.sequenceiq.flow.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventConverterAdapter<E extends FlowEvent> implements EventConverter<E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventConverterAdapter.class);

    private final Class<E> type;

    public EventConverterAdapter(Class<E> clazz) {
        type = clazz;
    }

    @Override
    public E convert(String key) {
        for (E event : type.getEnumConstants()) {
            if (key.equalsIgnoreCase(event.event())) {
                return event;
            }
        }

        String message = String.format("Cannot convert %s enum type to %s key, most probably this is an event from a different flow!", type, key);
        LOGGER.error(message);
        throw new IllegalArgumentException(message);
    }
}
