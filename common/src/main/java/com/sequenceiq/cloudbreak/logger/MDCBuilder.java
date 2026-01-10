package com.sequenceiq.cloudbreak.logger;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.util.ReflectionUtils;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

public class MDCBuilder {

    public static final String MDC_CONTEXT_ID = "MDC_CONTEXT_ID";

    private static final Logger LOGGER = LoggerFactory.getLogger(MDCBuilder.class);

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
        if (flowId != null) {
            MDC.put(LoggerContextKey.FLOW_ID.toString(), flowId);
        }
    }

    public static void addRequestId(String requestId) {
        MDC.put(LoggerContextKey.REQUEST_ID.toString(), requestId);
    }

    public static void addRequestId(Optional<String> requestId) {
        if (requestId.isPresent()) {
            MDC.put(LoggerContextKey.REQUEST_ID.toString(), requestId.get());
        }
    }

    public static String getOrGenerateRequestId() {
        String requestId = MDC.get(LoggerContextKey.REQUEST_ID.toString());
        if (null == requestId) {
            requestId = generateRequestId();
            LOGGER.debug("No requestId found. Setting request id to new UUID [{}]", requestId);
            MDC.put(LoggerContextKey.REQUEST_ID.toString(), requestId);
        }
        return requestId;
    }

    private static String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    public static void addEnvCrn(String env) {
        MDC.put(LoggerContextKey.ENVIRONMENT_ID.toString(), env);
    }

    public static void addEnvironmentCrn(String env) {
        MDC.put(LoggerContextKey.ENVIRONMENT_CRN.toString(), env);
    }

    public static void addResourceCrn(String crn) {
        MDC.put(LoggerContextKey.RESOURCE_CRN.toString(), crn);
    }

    public static void addAccountId(String account) {
        MDC.put(LoggerContextKey.ACCOUNT_ID.toString(), account);
    }

    public static void addTenant(String tenant) {
        MDC.put(LoggerContextKey.TENANT.toString(), tenant);
    }

    public static void addTraceId(String traceId) {
        MDC.put(LoggerContextKey.TRACE_ID.toString(), traceId);
    }

    public static void addSpanId(String spanId) {
        MDC.put(LoggerContextKey.SPAN_ID.toString(), spanId);
    }

    public static void addOperationId(String operationId) {
        MDC.put(LoggerContextKey.OPERATION_ID.toString(), operationId);
    }

    public static void removeOperationId() {
        removeMdcField(LoggerContextKey.OPERATION_ID.toString());
    }

    public static void buildMdcContext(Object object) {
        if (object == null) {
            MDC.put(LoggerContextKey.USER_CRN.toString(), null);
            MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), null);
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), null);
            MDC.put(LoggerContextKey.RESOURCE_CRN.toString(), null);
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), null);
            MDC.put(LoggerContextKey.CLIENT_ID.toString(), null);
        } else {
            MDC.put(LoggerContextKey.WORKSPACE.toString(), getFieldValue(object, "workspace"));
            MdcContext.builder()
                    .resourceCrn(getFieldValues(object, LoggerContextKey.RESOURCE_CRN.toString(), LoggerContextKey.CRN.toString()))
                    .resourceType(getResourceType(object))
                    .resourceName(getFieldValues(object, LoggerContextKey.NAME.toString(), LoggerContextKey.CLUSTER_NAME.toString()))
                    .environmentCrn(getFieldValues(object, LoggerContextKey.ENVIRONMENT_CRN.toString(), LoggerContextKey.ENVIRONMENT_ID.toString(),
                            LoggerContextKey.ENVIRONMENT_ID.toString()))
                    .tenant(getFieldValue(object, LoggerContextKey.ACCOUNT_ID.toString()))
                    .clientId(getFieldValue(object, LoggerContextKey.CLIENT_ID.toString()))
                    .buildMdc();
        }
        getOrGenerateRequestId();
    }

    public static void buildMdcContextFromInfoProvider(MdcContextInfoProvider object) {
        if (object == null) {
            MDC.put(LoggerContextKey.USER_CRN.toString(), null);
            MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), null);
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), null);
            MDC.put(LoggerContextKey.RESOURCE_CRN.toString(), null);
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), null);
        } else {
            MDC.put(LoggerContextKey.WORKSPACE.toString(), object.getWorkspaceName());
            MdcContext.builder()
                    .resourceCrn(object.getResourceCrn())
                    .resourceType(object.getResourceType())
                    .resourceName(object.getResourceName())
                    .environmentCrn(object.getEnvironmentCrn())
                    .tenant(object.getTenantName())
                    .buildMdc();
        }
        getOrGenerateRequestId();
    }

    public static void buildMdcContext(MdcContextInfoProvider object) {
        buildMdcContextFromInfoProvider(object);
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
        getOrGenerateRequestId();
    }

    public static void buildMdcContextFromCrn(Crn userCrn) {
        if (userCrn != null) {
            MdcContext.builder()
                    .tenant(userCrn.getAccountId())
                    .userCrn(userCrn.toString())
                    .buildMdc();
        }
    }

    public static void buildMdcContextFromMap(Map<String, String> map) {
        cleanupMdc();
        if (map != null) {
            map.forEach(MDC::put);
        }
        getOrGenerateRequestId();
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
            ReflectionUtils.makeAccessible(privateStringField);
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
                ReflectionUtils.makeAccessible(field.get());
                return field.get().get(o).toString();
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static String getResourceType(Object object) {
        String typeName = object.getClass().getSimpleName().toUpperCase(Locale.ROOT);
        return typeName.endsWith("VIEW") ? StringUtils.substringBefore(typeName, "VIEW") : typeName;
    }

    public static void cleanupMdc() {
        Arrays.stream(LoggerContextKey.values()).forEach(lck -> MDC.remove(lck.toString()));
    }
}
