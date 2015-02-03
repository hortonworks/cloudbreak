package com.sequenceiq.cloudbreak.logger;

import java.lang.reflect.Field;

import org.slf4j.MDC;

public class MDCBuilder {

    private MDCBuilder() {

    }

    public static void buildMdcContext() {
        buildMdcContext(null);
    }

    public static void buildMdcContext(Object object) {
        if (object == null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), "cloudbreak");
            MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), "cloudbreakLog");
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), "undefined");
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), "cb");
        } else {
            try {
                Field privateStringField = object.getClass().getDeclaredField("owner");
                privateStringField.setAccessible(true);
                MDC.put(LoggerContextKey.OWNER_ID.toString(), (String) privateStringField.get(object));
            } catch (Exception e) {
                MDC.put(LoggerContextKey.OWNER_ID.toString(), "undefined");
            }
            try {
                MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), object.getClass().getSimpleName().toUpperCase());
            } catch (Exception e) {
                MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), "undefined");
            }
            try {
                Field privateStringField = object.getClass().getDeclaredField("name");
                privateStringField.setAccessible(true);
                MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), (String) privateStringField.get(object));
            } catch (Exception e) {
                MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), "undefined");
            }
            try {
                Field privateStringField = object.getClass().getDeclaredField("id");
                privateStringField.setAccessible(true);
                MDC.put(LoggerContextKey.RESOURCE_ID.toString(), privateStringField.get(object).toString());
            } catch (Exception e) {
                MDC.put(LoggerContextKey.RESOURCE_ID.toString(), "undefined");
            }
        }
    }
}
