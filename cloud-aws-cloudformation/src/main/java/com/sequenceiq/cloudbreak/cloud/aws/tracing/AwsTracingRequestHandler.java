package com.sequenceiq.cloudbreak.cloud.aws.tracing;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.handlers.HandlerContextKey;
import com.amazonaws.handlers.RequestHandler2;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

/**
 * Tracing Request Handler
 */
public class AwsTracingRequestHandler extends RequestHandler2 {

    private final HandlerContextKey<Span> contextKey = new HandlerContextKey<>("span");

    private final SpanContext parentContext;

    private final Tracer tracer;

    public AwsTracingRequestHandler(Tracer tracer) {
        this.parentContext = null;
        this.tracer = tracer;
    }

    /**
     * In case of Async Client:  beforeRequest runs in separate thread therefore we need to inject
     * parent context to build chain
     *
     * @param parentContext parent context
     */
    public AwsTracingRequestHandler(SpanContext parentContext, Tracer tracer) {
        this.parentContext = parentContext;
        this.tracer = tracer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeRequest(Request<?> request) {
        Tracer.SpanBuilder spanBuilder = tracer
                .buildSpan("AWS - " + request.getOriginalRequest().getClass().getSimpleName())
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

        if (parentContext != null) {
            spanBuilder.asChildOf(parentContext);
        }

        Span span = spanBuilder.start();
        AwsSpanDecorator.onRequest(request, span);

        request.addHandlerContext(contextKey, span);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterResponse(Request<?> request, Response<?> response) {
        Span span = request.getHandlerContext(contextKey);
        AwsSpanDecorator.onResponse(response, span);
        span.finish();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterError(Request<?> request, Response<?> response, Exception e) {
        Span span = request.getHandlerContext(contextKey);
        AwsSpanDecorator.onError(e, span);
        span.finish();
    }
}
