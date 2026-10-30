package com.sequenceiq.environment.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EndpointConfig.class)
@TestPropertySource(locations = "file:./build/resources/main/application.properties",
        properties = {"environment.structuredevent.rest.enabled=false",
                "cb.structuredevent.rest.enabled=false"})
class EnvironmentMaintenanceWindowScheduleOpenApiTest {

    private static final String ENV_REQUEST = "EnvironmentV1Request";

    private static final String ENV_EDIT = "EnvironmentEditV1Request";

    private static final String ENV_DETAILED = "DetailedEnvironmentV1Response";

    private static final String SCHEDULE_REQUEST = "EnvironmentMaintenanceWindowScheduleV1Request";

    private static final String SCHEDULE_EDIT = "EnvironmentMaintenanceWindowScheduleEditV1Request";

    private static final String SCHEDULE_RESPONSE = "EnvironmentMaintenanceWindowScheduleV1Response";

    @MockBean
    private ExceptionMapper<?> exceptionMapper;

    @SpyBean
    private OpenApiProvider openApiProvider;

    @Autowired
    private EndpointConfig endpointConfig;

    @Test
    void openApiDocumentsMaintenanceWindowScheduleOnCreateEditAndDescribe() throws Exception {
        OpenAPI openAPI = readOpenApi();
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();

        assertThat(schemas).containsKeys(SCHEDULE_REQUEST, SCHEDULE_EDIT, SCHEDULE_RESPONSE);
        assertThat(schemaProperties(schemas, ENV_REQUEST)).containsKey("maintenanceWindowSchedule");
        assertThat(schemaProperties(schemas, ENV_EDIT)).containsKeys("maintenanceWindowSchedule", "clearMaintenanceWindowSchedule");
        assertThat(schemaProperties(schemas, ENV_DETAILED)).containsKey("maintenanceWindowSchedule");
    }

    @SuppressWarnings("rawtypes")
    private static Map<String, Schema> schemaProperties(Map<String, Schema> schemas, String name) {
        Schema schema = schemas.get(name);
        assertThat(schema).as("schema %s", name).isNotNull();
        return schema.getProperties();
    }

    private OpenAPI readOpenApi() throws Exception {
        return JaxrsOpenApiTestReader.read(endpointConfig.getClasses());
    }
}
