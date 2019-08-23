package com.sequenceiq.cloudbreak.tracing;

import java.util.List;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.client.ClientSpanDecorator;
import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.opentracing.contrib.jaxrs2.server.ServerSpanDecorator;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;

@Configuration
public class TracingConfiguration {

    private final Tracer tracer;

    public TracingConfiguration(Tracer tracer) {
        this.tracer = tracer;
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

    public static class SpanDecorator implements ServerSpanDecorator, ClientSpanDecorator {

        @Override
        public void decorateRequest(ContainerRequestContext requestContext, Span span) {
            MDCBuilder.addTraceId(span.context().toTraceId());
            MDCBuilder.addSpanId(span.context().toSpanId());
        }

        @Override
        public void decorateResponse(ContainerResponseContext responseContext, Span span) {

        }

        @Override
        public void decorateRequest(ClientRequestContext requestContext, Span span) {
            MDCBuilder.addTraceId(span.context().toTraceId());
            MDCBuilder.addSpanId(span.context().toSpanId());
        }

        @Override
        public void decorateResponse(ClientResponseContext responseContext, Span span) {

        }
    }
}
