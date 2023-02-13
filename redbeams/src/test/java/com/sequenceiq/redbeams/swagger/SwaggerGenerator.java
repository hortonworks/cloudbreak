package com.sequenceiq.redbeams.swagger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.ext.ExceptionMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.service.openapi.OpenApiProvider;
import com.sequenceiq.redbeams.configuration.EndpointConfig;
import com.sequenceiq.redbeams.configuration.SwaggerConfig;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.models.OpenAPI;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {EndpointConfig.class, SwaggerConfig.class})
@TestPropertySource(locations = {"file:./build/resources/main/application.properties", "file:./build/resources/main/application.yml"})
public class SwaggerGenerator {

    @MockBean
    private ExceptionMapper<?> exceptionMapper;

    @SpyBean
    private OpenApiProvider openApiProvider;

    @Autowired
    private EndpointConfig endpointConfig;

    @Autowired
    private SwaggerConfig swaggerConfig;

    @Test
    public void generateSwaggerJson() throws Exception {
        Set<String> classes = endpointConfig.getClasses().stream()
                .map(Class::getName)
                .collect(Collectors.toSet());

        OpenAPI openAPI = new JaxrsOpenApiContextBuilder<>()
                .resourceClasses(classes)
                .buildContext(true)
                .read();
        Path path = Paths.get("./build/swagger/redbeams.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, Json.pretty(openAPI));
    }

}