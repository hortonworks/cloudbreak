package com.sequenceiq.cloudbreak.reactor.api.event;

public class EventSelectorUtil {
    private EventSelectorUtil() {
    }

    public static String selector(Class clazz) {
        return clazz.getSimpleName().toUpperCase();
    }

    public static String failureSelector(Class clazz) {
        return clazz.getSimpleName().toUpperCase() + "_ERROR";
    }
}
