package com.sequenceiq.flow.event;

import java.util.Locale;

public class EventSelectorUtil {
    private EventSelectorUtil() {
    }

    public static String selector(Class<?> clazz) {
        return clazz.getSimpleName().toUpperCase(Locale.ROOT);
    }

    public static String failureSelector(Class<?> clazz) {
        return clazz.getSimpleName().toUpperCase(Locale.ROOT) + "_ERROR";
    }
}
