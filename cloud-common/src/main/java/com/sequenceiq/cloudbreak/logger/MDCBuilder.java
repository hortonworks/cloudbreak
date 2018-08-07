package com.sequenceiq.cloudbreak.logger;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;

public class MDCBuilder {
    public static final String MDC_CONTEXT_ID = "MDC_CONTEXT_ID";

    private MDCBuilder() {
    }

    public static void buildMdcContext() {
        buildMdcContext(null);
    }

    public static void addFlowIdToMdcContext(String flowId) {
        MDC.put(LoggerContextKey.FLOW_ID.toString(), flowId);
    }

    public static void addTrackingIdToMdcContext(String trackingId) {
        MDC.put(LoggerContextKey.TRACKING_ID.toString(), trackingId);
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

    public static void buildMdcContext(String stackId, String stackName, String ownerId, String type) {
        MDC.put(LoggerContextKey.OWNER_ID.toString(), StringUtils.isEmpty(ownerId) ? "" : ownerId);
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), StringUtils.isEmpty(stackId) ? "" : stackId);
        MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), StringUtils.isEmpty(stackName) ? "" : stackName);
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), StringUtils.isEmpty(type) ? "" : type);
    }

    public static void buildUserMdcContext(IdentityUser user) {
        if (user != null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), user.getUserId());
        }
    }

    public static void buildMdcContextFromMap(Map<String, String> map) {
        cleanupMdc();
        if (map != null) {
            map.forEach(MDC::put);
        }
    }

    public static Map<String, String> getMdcContextMap() {
        Map<String, String> result = Maps.newHashMap();
        for (LoggerContextKey lck : LoggerContextKey.values()) {
            putIfExist(result, lck);
        }
        return result;
    }

    private static void putIfExist(Map<String, String> map, LoggerContextKey lck) {
        String lckStr = lck.toString();
        String mdcParam = MDC.get(lckStr);
        if (!StringUtils.isEmpty(mdcParam)) {
            map.put(lckStr, mdcParam);
        }
    }

    static String getFieldValue(Object o, String field) {
        try {
            Field privateStringField = ReflectionUtils.findField(o.getClass(), field);
            privateStringField.setAccessible(true);
            return privateStringField.get(o).toString();
        } catch (Exception ignored) {
            return "undefined";
        }
    }

    public static void cleanupMdc() {
        Arrays.stream(LoggerContextKey.values()).forEach(lck -> MDC.remove(lck.toString()));
    }
}
