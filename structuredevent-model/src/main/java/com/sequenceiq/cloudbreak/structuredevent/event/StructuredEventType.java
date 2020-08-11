package com.sequenceiq.cloudbreak.structuredevent.event;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;

public enum StructuredEventType {

    REST(StructuredRestCallEvent.class, CDPStructuredRestCallEvent.class),
    FLOW(StructuredFlowEvent.class, CDPStructuredFlowEvent.class),
    NOTIFICATION(StructuredNotificationEvent.class, CDPStructuredNotificationEvent.class);

    private static final Map<Class<? extends StructuredEvent>, StructuredEventType> STRUCTURED_EVENT_TYPE_MAP = new HashMap<>();

    private static final Map<Class<? extends CDPStructuredEvent>, StructuredEventType> CDP_STRUCTURED_EVENT_TYPE_MAP = new HashMap<>();

    static {
        for (StructuredEventType se : values()) {
            STRUCTURED_EVENT_TYPE_MAP.put(se.clazz, se);
            CDP_STRUCTURED_EVENT_TYPE_MAP.put(se.cdpClazz, se);
        }
    }

    private final Class<? extends StructuredEvent> clazz;
    private final Class<? extends CDPStructuredEvent> cdpClazz;

    StructuredEventType(Class<? extends StructuredEvent> clazz, Class<? extends CDPStructuredEvent> cdpClazz) {
        this.clazz = clazz;
        this.cdpClazz = cdpClazz;
    }

    public static StructuredEventType getByClass(Class<? extends StructuredEvent> clazz) {
        return STRUCTURED_EVENT_TYPE_MAP.get(clazz);
    }

    public static StructuredEventType getByCDPClass(Class<? extends CDPStructuredEvent> clazz) {
        return STRUCTURED_EVENT_TYPE_MAP.get(clazz);
    }
}
