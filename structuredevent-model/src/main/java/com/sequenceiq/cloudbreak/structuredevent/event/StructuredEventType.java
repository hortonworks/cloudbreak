package com.sequenceiq.cloudbreak.structuredevent.event;

import java.util.HashMap;
import java.util.Map;

public enum StructuredEventType {

    REST(StructuredRestCallEvent.class), FLOW(StructuredFlowEvent.class), NOTIFICATION(StructuredNotificationEvent.class);

    private static final Map<Class<? extends StructuredEvent>, StructuredEventType> STRUCTURED_EVENT_TYPE_MAP = new HashMap<>();

    static {
        for (StructuredEventType se : values()) {
            STRUCTURED_EVENT_TYPE_MAP.put(se.clazz, se);
        }
    }

    private final Class<? extends StructuredEvent> clazz;

    StructuredEventType(Class<? extends StructuredEvent> clazz) {
        this.clazz = clazz;
    }

    public static StructuredEventType getByClass(Class<? extends StructuredEvent> clazz) {
        return STRUCTURED_EVENT_TYPE_MAP.get(clazz);
    }
}
