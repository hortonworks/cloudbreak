package com.sequenceiq.cloudbreak.logger;

import java.lang.reflect.Field;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;

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
            MDC.put(LoggerContextKey.OWNER_ID.toString(), getFieldValue(object, "owner"));
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), getFieldValue(object, "id"));
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), getFieldValue(object, "name"));
            MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), object.getClass().getSimpleName().toUpperCase());
        }
    }

    public static void buildMdcContext(String stackId, String stackName, String ownerId) {
        MDC.put(LoggerContextKey.OWNER_ID.toString(), ownerId);
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), stackId);
        MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), stackName);
    }

    public static void buildUserMdcContext(IdentityUser user) {
        if (user != null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), user.getUserId());
        }
    }

    public static void buildMdcContextFromMap(Map<String, String> map) {
        cleanupMdc();
        map.entrySet().stream().forEach(e -> MDC.put(e.getKey(), e.getValue()));
    }

    static String getFieldValue(Object o, String field) {
        try {
            Field privateStringField = ReflectionUtils.findField(o.getClass(), field);
            privateStringField.setAccessible(true);
            return privateStringField.get(o).toString();
        } catch (Exception e) {
            return "undefined";
        }
    }

    private static void cleanupMdc() {
        MDC.remove(LoggerContextKey.OWNER_ID.toString());
        MDC.remove(LoggerContextKey.RESOURCE_ID.toString());
        MDC.remove(LoggerContextKey.RESOURCE_NAME.toString());
        MDC.remove(LoggerContextKey.RESOURCE_TYPE.toString());
    }
}
