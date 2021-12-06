package com.sequenceiq.cloudbreak.tracing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.client.ClientSpanDecorator;
import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.opentracing.contrib.jaxrs2.server.ServerSpanDecorator;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;
import io.opentracing.contrib.jdbc.TracingDriver;
import io.opentracing.util.GlobalTracer;

@Configuration
public class TracingConfiguration {

    private final Tracer tracer;

    private final Set<String> allowedHeaderTags;

    @Value("${opentracing.jdbc.enabled}")
    private boolean jdbcEnabled;

    public TracingConfiguration(Tracer tracer, @Value("#{'${opentracing.allowed-header-tags}'.split(',')}") Set<String> allowedHeaderTags) {
        this.tracer = tracer;
        this.allowedHeaderTags = allowedHeaderTags;
        GlobalTracer.registerIfAbsent(tracer);
    }

    @PostConstruct
    public void initTracingProperties() {
        TracingDriver.setTraceEnabled(jdbcEnabled);
        TracingDriver.setInterceptorProperty(true);
        TracingDriver.setInterceptorMode(true);
    }

    @Bean
    public ServerTracingDynamicFeature serverTracingDynamicFeature() {
        ServerTracingDynamicFeature.Builder serverTracingFeatureBuilder = new ServerTracingDynamicFeature.Builder(tracer);
        serverTracingFeatureBuilder.withTraceSerialization(false);
        serverTracingFeatureBuilder.withDecorators(List.of(new SpanDecorator()));
        return serverTracingFeatureBuilder.build();
    }

    @Bean
    public ClientTracingFeature clientTracingFeature() {
        ClientTracingFeature.Builder clientTracingFeatureBuilder = new ClientTracingFeature.Builder(tracer);
        clientTracingFeatureBuilder.withTraceSerialization(false);
        clientTracingFeatureBuilder.withDecorators(List.of(new SpanDecorator()));
        return clientTracingFeatureBuilder.build();
    }

    public boolean isJdbcTracingEnabled() {
        return jdbcEnabled;
    }

    public class SpanDecorator implements ServerSpanDecorator, ClientSpanDecorator {

        @Override
        public void decorateRequest(ContainerRequestContext requestContext, Span span) {
            doDecorateRequest(new HashMap<>(requestContext.getHeaders()), span);
        }

        @Override
        public void decorateResponse(ContainerResponseContext responseContext, Span span) {
            doDecorateResponse(span, responseContext.getStatusInfo());
        }

        @Override
        public void decorateRequest(ClientRequestContext requestContext, Span span) {
            doDecorateRequest(new HashMap<>(requestContext.getHeaders()), span);
        }

        @Override
        public void decorateResponse(ClientResponseContext responseContext, Span span) {
            doDecorateResponse(span, responseContext.getStatusInfo());
        }

        private void doDecorateRequest(Map<String, Object> headers, Span span) {
            MDCBuilder.addTraceId(span.context().toTraceId());
            MDCBuilder.addSpanId(span.context().toSpanId());
            TracingUtil.setTagsFromMdc(span);
            headers.keySet().retainAll(allowedHeaderTags);
            span.setTag(TracingUtil.HEADERS, Json.silent(headers).getValue());
        }

        private void doDecorateResponse(Span span, Response.StatusType statusInfo2) {
            MDCBuilder.addTraceId(span.context().toTraceId());
            MDCBuilder.addSpanId(span.context().toSpanId());
            Response.StatusType statusInfo = statusInfo2;
            if (statusInfo.getFamily() != Response.Status.Family.SUCCESSFUL) {
                span.setTag(TracingUtil.ERROR, true);
                span.log(Map.of(TracingUtil.RESPONSE_CODE, statusInfo.getStatusCode(), "reasonPhrase", statusInfo.getReasonPhrase()));
            }
        }
    }
}
