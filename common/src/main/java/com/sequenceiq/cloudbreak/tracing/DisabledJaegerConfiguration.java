package com.sequenceiq.cloudbreak.tracing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;

@ConditionalOnProperty(value = "opentracing.jaeger.enabled", havingValue = "false", matchIfMissing = false)
@Configuration
public class DisabledJaegerConfiguration {

    @Bean
    public Tracer tracer() {
        return NoopTracerFactory.create();
    }
}
