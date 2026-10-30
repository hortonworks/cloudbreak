package com.sequenceiq.cloudbreak.service.openapi;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * Loads OpenAPI from registered JAX-RS resource classes (same path as {@code OpenApiGenerator} tests).
 */
public final class JaxrsOpenApiTestReader {

    private JaxrsOpenApiTestReader() {
    }

    public static OpenAPI read(Collection<Class<?>> resourceClasses) throws Exception {
        Set<String> classNames = resourceClasses.stream().map(Class::getName).collect(Collectors.toSet());
        return new JaxrsOpenApiContextBuilder<>()
                .resourceClasses(classNames)
                .buildContext(true)
                .read();
    }
}
