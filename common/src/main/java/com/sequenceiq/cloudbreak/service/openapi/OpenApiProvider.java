package com.sequenceiq.cloudbreak.service.openapi;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.ACTOR_CRN_HEADER;
import static com.sequenceiq.cloudbreak.constant.OpenApiConstants.INFO_CONTACT_NAME;
import static com.sequenceiq.cloudbreak.constant.OpenApiConstants.INFO_CONTACT_URL;
import static com.sequenceiq.cloudbreak.constant.OpenApiConstants.INFO_LICENSE_TYPE;
import static com.sequenceiq.cloudbreak.constant.OpenApiConstants.INFO_LICENSE_URL;
import static io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Component
public class OpenApiProvider {

    public OpenAPI getOpenAPI(String title, String description, String applicationVersion, String server) {
        OpenAPI openAPI = new OpenAPI()
            .specVersion(SpecVersion.V30)
            .info(new Info()
                .title(title)
                .description(description)
                .version(applicationVersion)
                .contact(new Contact()
                        .name(INFO_CONTACT_NAME)
                        .url(INFO_CONTACT_URL))
                .license(new License()
                        .name(INFO_LICENSE_TYPE)
                        .url(INFO_LICENSE_URL)))
            .servers(List.of(new Server().url(server)))
            .components(getComponents());
        return openAPI;
    }

    private Components getComponents() {
        Components components = new Components();
        SecurityScheme securityScheme = new SecurityScheme();
        securityScheme.setType(SecurityScheme.Type.APIKEY);
        securityScheme.setName(ACTOR_CRN_HEADER);
        securityScheme.setIn(HEADER);
        components.setSecuritySchemes(Map.of("Authorization", securityScheme));
        return components;
    }

    public void createConfig(OpenAPI openAPI, Set<String> resourceClassNames) {
        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
                .cacheTTL(0L)
                .openAPI(openAPI)
                .prettyPrint(true)
                .readAllResources(false)
                .resourceClasses(resourceClassNames);

        try {
            new JaxrsOpenApiContextBuilder<>()
                    .openApiConfiguration(oasConfig)
                    .buildContext(true);
        } catch (OpenApiConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
