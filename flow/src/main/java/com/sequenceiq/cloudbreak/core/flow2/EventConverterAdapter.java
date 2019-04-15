package com.sequenceiq.cloudbreak.core.flow2;

public class EventConverterAdapter<E extends FlowEvent> implements EventConverter<E> {

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
        return null;
    }
}
