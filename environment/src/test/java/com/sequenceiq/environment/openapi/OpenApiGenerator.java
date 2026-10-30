package com.sequenceiq.environment.openapi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.ws.rs.ext.ExceptionMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.service.openapi.JaxrsOpenApiTestReader;
import com.sequenceiq.cloudbreak.service.openapi.OpenApiProvider;
import com.sequenceiq.environment.configuration.api.EndpointConfig;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EndpointConfig.class)
@TestPropertySource(locations = "file:./build/resources/main/application.properties",
        properties = {"environment.structuredevent.rest.enabled=false",
                "cb.structuredevent.rest.enabled=false"})
public class OpenApiGenerator {

    @MockBean
    private ExceptionMapper<?> exceptionMapper;

    @SpyBean
    private OpenApiProvider openApiProvider;

    @Autowired
    private EndpointConfig endpointConfig;

    @Test
    public void generateSwaggerJson() throws Exception {
        OpenAPI openAPI = JaxrsOpenApiTestReader.read(endpointConfig.getClasses());
        Path path = Paths.get("./build/openapi/environment.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, Json.pretty(openAPI));
    }

}
