package com.sequenceiq.cloudbreak.logger;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.MDC;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.altus.Crn;

public class MDCBuilder {
    public static final String MDC_CONTEXT_ID = "MDC_CONTEXT_ID";

    private MDCBuilder() {
    }

    public static void buildMdcContext() {
        buildMdcContext(null);
    }

    public static void addMdcField(String field, String value) {
        MDC.put(field, value);
    }

    public static void removeMdcField(String field) {
        MDC.remove(field);
    }

    public static void addFlowId(String flowId) {
        MDC.put(LoggerContextKey.FLOW_ID.toString(), flowId);
    }

    public static void addRequestId(String requestId) {
        MDC.put(LoggerContextKey.REQUEST_ID.toString(), requestId);
    }

    public static void addEnvCrn(String env) {
        MDC.put(LoggerContextKey.ENV_CRN.toString(), env);
    }

    public static void addResourceCrn(String crn) {
        MDC.put(LoggerContextKey.RESOURCE_CRN.toString(), crn);
    }

    public static void addAccountId(String account) {
        MDC.put(LoggerContextKey.ACCOUNT_ID.toString(), account);
    }

    public static void addTraceId(String traceId) {
        MDC.put(LoggerContextKey.TRACE_ID.toString(), traceId);
    }

    public static void addSpanId(String spanId) {
        MDC.put(LoggerContextKey.SPAN_ID.toString(), spanId);
    }

    public static void buildMdcContext(Object object) {
        if (object == null) {
            MDC.put(LoggerContextKey.USER_CRN.toString(), null);
            MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), null);
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), null);
            MDC.put(LoggerContextKey.RESOURCE_CRN.toString(), null);
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), null);
        } else {
            MDC.put(LoggerContextKey.WORKSPACE.toString(), getFieldValue(object, "workspace"));
            MdcContext.builder()
                    .resourceCrn(getFieldValues(object, LoggerContextKey.RESOURCE_CRN.toString(), LoggerContextKey.CRN.toString()))
                    .resourceType(object.getClass().getSimpleName().toUpperCase())
                    .resourceName(getFieldValues(object, LoggerContextKey.NAME.toString(), LoggerContextKey.CLUSTER_NAME.toString()))
                    .environmentCrn(getFieldValues(object, LoggerContextKey.ENVIRONMENT_CRN.toString(), LoggerContextKey.ENV_CRN.toString()))
                    .tenant(getFieldValue(object, LoggerContextKey.ACCOUNT_ID.toString()))
                    .buildMdc();
        }
    }

    public static void buildMdcContext(Long resourceId, String resourceName, String type) {
        buildMdcContext(resourceId == null ? "" : String.valueOf(resourceId), resourceName, type);
    }

    public static void buildMdcContext(String resourceId, String resourceName, String type) {
        MdcContext.builder()
                .resourceCrn(resourceId)
                .resourceName(resourceName)
                .resourceType(type)
                .buildMdc();
    }

    public static void buildMdc(MdcContext mdcContext) {
        doIfNotNull(mdcContext.getTenant(), v -> MDC.put(LoggerContextKey.TENANT.toString(), v));
        doIfNotNull(mdcContext.getUserCrn(), v -> MDC.put(LoggerContextKey.USER_CRN.toString(), v));
        doIfNotNull(mdcContext.getEnvironmentCrn(), v -> MDC.put(LoggerContextKey.ENVIRONMENT_CRN.toString(), v));
        doIfNotNull(mdcContext.getFlowId(), v -> MDC.put(LoggerContextKey.FLOW_ID.toString(), v));
        doIfNotNull(mdcContext.getRequestId(), v -> MDC.put(LoggerContextKey.REQUEST_ID.toString(), v));
        doIfNotNull(mdcContext.getResourceCrn(), v -> MDC.put(LoggerContextKey.RESOURCE_CRN.toString(), v));
        doIfNotNull(mdcContext.getResourceName(), v -> MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), v));
        doIfNotNull(mdcContext.getResourceType(), v -> MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), v));
        doIfNotNull(mdcContext.getTraceId(), v -> MDC.put(LoggerContextKey.TRACE_ID.toString(), v));
        doIfNotNull(mdcContext.getSpanId(), v -> MDC.put(LoggerContextKey.SPAN_ID.toString(), v));
    }

    public static void buildMdcContextFromCrn(Crn crn) {
        if (crn != null) {
            MdcContext.builder()
                    .tenant(crn.getAccountId())
                    .userCrn(crn.toString())
                    .buildMdc();
        }
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

    public static String getFieldValue(Object o, String field) {
        try {
            Field privateStringField = FieldUtils.getField(o.getClass(), field, true);
            privateStringField.setAccessible(true);
            return privateStringField.get(o).toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String getFieldValues(Object o, String... fields) {
        try {
            Optional<Field> field = Stream.of(fields)
                    .map(f -> FieldUtils.getField(o.getClass(), f, true))
                    .filter(Objects::nonNull)
                    .findFirst();
            if (field.isPresent()) {
                field.get().setAccessible(true);
                return field.get().get(o).toString();
            }
        } catch (Exception ignored) {

        }
        return null;
    }

    public static void cleanupMdc() {
        Arrays.stream(LoggerContextKey.values()).forEach(lck -> MDC.remove(lck.toString()));
    }
}
