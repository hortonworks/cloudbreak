package com.sequenceiq.redbeams.configuration;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.service.openapi.OpenApiProvider;
import com.sequenceiq.redbeams.api.RedbeamsApi;

import io.swagger.v3.oas.models.OpenAPI;

@Configuration
public class SwaggerConfig {

    private static final Set<String> OPENAPI_RESOURCE_PACKAGES = Stream.of(
            "com.sequenceiq.redbeams.api",
                    "com.sequenceiq.flow.api",
                    "com.sequenceiq.authorization")
            .collect(Collectors.toSet());

    @Value("${info.app.version:unspecified}")
    private String applicationVersion;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Inject
    private OpenApiProvider openApiProvider;

    @PostConstruct
    public void registerSwagger() {
        OpenAPI openAPI = openApiProvider.getOpenAPI(
                "Redbeams API",
                "API for working with databases and database servers",
                applicationVersion,
                "https://localhost" + contextPath + RedbeamsApi.API_ROOT_CONTEXT
        );
        openAPI.setComponents(openApiProvider.getComponents());
        openApiProvider.createConfig(openAPI, OPENAPI_RESOURCE_PACKAGES);
    }
}
