package com.sequenceiq.maintenance.openapi;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.sequenceiq.maintenance.configuration.api.EndpointConfig;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EndpointConfig.class)
@TestPropertySource(locations = "file:./build/resources/main/application.yml")
class MaintenanceWindowScheduleOpenApiTest {

    @MockBean
    private ExceptionMapper<?> exceptionMapper;

    @SpyBean
    private OpenApiProvider openApiProvider;

    @Autowired
    private EndpointConfig endpointConfig;

    @Test
    void schedulePathsMatchPublicV1Contract() throws Exception {
        OpenAPI openAPI = readOpenApi();

        assertThat(openAPI.getPaths()).containsKey("/v1/maintenance/schedules");
        PathItem byScope = openAPI.getPaths().get("/v1/maintenance/schedules/scope/{scopeType}/scopeId/{scopeId}");
        assertThat(byScope.getGet()).isNotNull();
        assertThat(byScope.getPatch()).isNotNull();
        assertThat(byScope.getDelete()).isNotNull();
        assertThat(openAPI.getPaths().get("/v1/maintenance/schedules/scope/{scopeType}/scopeId/{scopeId}/skip-next").getPost())
                .isNotNull();
        assertThat(openAPI.getPaths().get("/v1/maintenance/schedules/scope/{scopeType}/scopeId/{scopeId}/skip-next").getDelete())
                .isNotNull();
    }

    private OpenAPI readOpenApi() throws Exception {
        return JaxrsOpenApiTestReader.read(endpointConfig.getClasses());
    }
}
