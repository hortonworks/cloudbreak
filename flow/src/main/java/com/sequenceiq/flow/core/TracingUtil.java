package com.sequenceiq.flow.core;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;

public class TracingUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(TracingUtil.class);

    private TracingUtil() {
    }

    public static SpanContext useOrCreateSpanContext(SpanContext spanContext, Span span) {
        LOGGER.debug("Creating a new span. {}", span.context());
        if (spanContext == null) {
            LOGGER.debug("Using the new span context. {}", span.context());
            spanContext = span.context();
        }
        return spanContext;
    }

    public static Span getSpan(Tracer tracer, String operationName, SpanContext spanContext, String flowId, String flowChainId, String flowTriggerUserCrn) {
        Span span = tracer
                .buildSpan(operationName)
                .addReference(References.FOLLOWS_FROM, spanContext)
                .ignoreActiveSpan()
                .start();
        span.setBaggageItem(FlowConstants.OPERATION_NAME, operationName);
        MDCBuilder.addSpanId(span.context().toSpanId());
        MDCBuilder.addTraceId(span.context().toTraceId());
        span.setTag(FlowConstants.FLOW_ID, flowId);
        span.setTag(FlowConstants.FLOW_CHAIN_ID, flowChainId);
        span.setTag(FlowConstants.FLOW_TRIGGER_USERCRN, flowTriggerUserCrn);
        span.setTag("RESOURCE_CRN", MDC.get("resourceCrn"));
        return span;
    }

    public static boolean isActiveSpanReusable(Span activeSpan, SpanContext spanContext, String operationName) {
        boolean reusable = activeSpan != null && spanContext != null
                && StringUtils.equals(activeSpan.getBaggageItem(FlowConstants.OPERATION_NAME), operationName)
                && activeSpan.context().toTraceId().equals(spanContext.toTraceId());
        if (reusable) {
            MDCBuilder.addSpanId(activeSpan.context().toSpanId());
            MDCBuilder.addTraceId(activeSpan.context().toTraceId());
        }
        return reusable;
    }
}
