package com.sequenceiq.cloudbreak.openapi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ext.ExceptionMapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.controller.EndpointConfig;
import com.sequenceiq.cloudbreak.service.openapi.OpenApiProvider;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.models.OpenAPI;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = EndpointConfig.class)
@TestPropertySource(locations = "file:./build/resources/main/application.properties",
        properties = {"cb.structuredevent.rest.enabled=false"})
public class OpenApiGenerator {

    @MockBean
    private ExceptionMapper<?> exceptionMapper;

    @SpyBean
    private OpenApiProvider openApiProvider;

    @Autowired
    private EndpointConfig endpointConfig;

    @Test
    public void generateSwaggerJson() throws Exception {
        Set<String> classes = endpointConfig.getClasses().stream()
                .map(Class::getName)
                .collect(Collectors.toSet());

        OpenAPI openAPI = new JaxrsOpenApiContextBuilder<>()
                .resourceClasses(classes)
                .buildContext(true)
                .read();

        Path path = Paths.get("./build/openapi/cb.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, Json.pretty(openAPI));
    }

}
