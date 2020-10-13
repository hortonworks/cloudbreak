package com.sequenceiq.cloudbreak.tracing;

import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.logger.LoggerContextKey;

import io.opentracing.Span;

public class TracingUtil {

    public static final String COMPONENT = "component";

    public static final String HTTP_METHOD = "http.method";

    public static final String HEADERS = "headers";

    public static final String ERROR = "error";

    public static final String RESPONSE_CODE = "responseCode";

    public static final String MESSAGE = "message";

    public static final String URL = "url";

    private TracingUtil() {
    }

    public static void setTagsFromMdc(Span span) {
        String environmentCrn = MDC.get(LoggerContextKey.ENVIRONMENT_CRN.toString());
        environmentCrn = environmentCrn == null ? MDC.get(LoggerContextKey.ENV_CRN.toString()) : environmentCrn;
        span.setTag(LoggerContextKey.ENVIRONMENT_CRN.name(), environmentCrn);
        span.setTag(LoggerContextKey.RESOURCE_CRN.name(), MDC.get(LoggerContextKey.RESOURCE_CRN.toString()));
        span.setTag(LoggerContextKey.REQUEST_ID.name(), MDC.get(LoggerContextKey.REQUEST_ID.toString()));
    }
}
