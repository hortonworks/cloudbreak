package com.sequenceiq.cloudbreak.logger;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.util.ReflectionUtils;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;

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

    public static void addRequestIdToMdcContext(String requestId) {
        MDC.put(LoggerContextKey.REQUEST_ID.toString(), requestId);
    }

    public static void buildMdcContext(Object object) {
        if (object == null) {
            MDC.put(LoggerContextKey.USER_ID.toString(), "undefined");
            MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), "undefined");
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), "undefined");
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), "undefined");
        } else {
            MDC.put(LoggerContextKey.WORKSPACE.toString(), getFieldValue(object, "workspace"));
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), getFieldValue(object, "id"));
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), getFieldValue(object, "name"));
            MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), object.getClass().getSimpleName().toUpperCase());
        }
    }

    public static void buildMdcContext(Long resourceId, String resourceName, String type) {
        buildMdcContext(resourceId == null ? "" : String.valueOf(resourceId), resourceName, type);
    }

    public static void buildMdcContext(String resourceId, String resourceName, String type) {
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), StringUtils.isEmpty(resourceId) ? "" : resourceId);
        MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), StringUtils.isEmpty(resourceName) ? "" : resourceName);
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), StringUtils.isEmpty(type) ? "" : type);
    }

    public static void buildUserMdcContext(CloudbreakUser user) {
        if (user != null) {
            MDC.put(LoggerContextKey.USER_ID.toString(), user.getUserId());
            MDC.put(LoggerContextKey.USER_NAME.toString(), user.getUsername());
            MDC.put(LoggerContextKey.TENANT.toString(), user.getTenant());
        }
    }

    public static void buildUserMdcContext(String userId, String userName) {
        if (StringUtils.isNotEmpty(userId)) {
            MDC.put(LoggerContextKey.USER_ID.toString(), userId);
        }
        if (StringUtils.isNotEmpty(userName)) {
            MDC.put(LoggerContextKey.USER_NAME.toString(), userName);
        }
    }

    public static void buildWorkspaceMdcContext(Long workspaceId) {
        MDC.put(LoggerContextKey.WORKSPACE_ID.toString(), workspaceId == null ? "undefined" : workspaceId.toString());
    }

    public static void buildMdcContextFromMap(Map<String, String> map) {
        cleanupMdc();
        if (map != null) {
            map.forEach(MDC::put);
        }
    }

    public static void buildMdcContextFromMapForControllerCalls(Map<String, String> map) {
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
