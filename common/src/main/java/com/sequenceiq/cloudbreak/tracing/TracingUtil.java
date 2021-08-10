package com.sequenceiq.cloudbreak.tracing;

import java.util.Optional;

import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;

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

    public static Span initSpan(Tracer tracer, String component, String operation) {
        Span activeSpan = tracer.activeSpan();
        return initFromActiveSpan(tracer, component, operation, activeSpan != null ? activeSpan.context() : null);
    }

    public static Optional<Span> initOptionalSpan(Tracer tracer, String component, String operation) {
        return Optional.ofNullable(tracer.activeSpan()).map(activeSpan ->
                initFromActiveSpan(tracer, component, operation, activeSpan.context()));
    }

    private static Span initFromActiveSpan(Tracer tracer, String component, String operation, SpanContext spanContext) {
        Span span = tracer.buildSpan(component + " - " + operation)
                .addReference(References.FOLLOWS_FROM, spanContext)
                .start();
        span.setTag(TracingUtil.COMPONENT, component);
        return span;
    }

    public static void setTagsFromMdc(Span span) {
        String environmentCrn = MDC.get(LoggerContextKey.ENVIRONMENT_CRN.toString());
        environmentCrn = environmentCrn == null ? MDC.get(LoggerContextKey.ENV_CRN.toString()) : environmentCrn;
        span.setTag(LoggerContextKey.ENVIRONMENT_CRN.name(), environmentCrn);
        span.setTag(LoggerContextKey.RESOURCE_CRN.name(), MDC.get(LoggerContextKey.RESOURCE_CRN.toString()));
        span.setTag(LoggerContextKey.REQUEST_ID.name(), MDCBuilder.getOrGenerateRequestId());
    }
}
