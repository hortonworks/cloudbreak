package com.sequenceiq.periscope.openapi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.service.openapi.OpenApiProvider;
import com.sequenceiq.periscope.config.EndpointConfig;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.models.OpenAPI;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EndpointConfig.class)
@TestPropertySource(locations = "file:./build/resources/main/application.properties")
public class OpenApiGenerator {

    @Autowired
    private EndpointConfig endpointConfig;

    @SpyBean
    private OpenApiProvider openApiProvider;

    @Test
    public void generateSwaggerJson() throws Exception {
        Set<String> classes = endpointConfig.getClasses().stream()
                .map(Class::getName)
                .collect(Collectors.toSet());

        OpenAPI openAPI = new JaxrsOpenApiContextBuilder<>()
                .resourceClasses(classes)
                .buildContext(true)
                .read();
        Path path = Paths.get("./build/openapi/autoscale.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, Json.pretty(openAPI));
    }

}
