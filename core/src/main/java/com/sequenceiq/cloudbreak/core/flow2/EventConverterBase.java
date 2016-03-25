package com.sequenceiq.cloudbreak.core.flow2;

public class EventConverterBase<E extends FlowEvent> implements EventConverter<E> {
    private Class<E> type;

    public EventConverterBase(Class<E> clazz) {
        type = clazz;
    }

    @Override
    public E convert(String key) {
        for (E event : type.getEnumConstants()) {
            if (key.equalsIgnoreCase(event.stringRepresentation())) {
                return event;
            }
        }
        return null;
    }
}
