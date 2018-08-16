package com.sequenceiq.cloudbreak.controller;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.controller.audit.AuditController;
import com.sequenceiq.cloudbreak.controller.mapper.DefaultExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.WebApplicaitonExceptionMapper;
import com.sequenceiq.cloudbreak.structuredevent.rest.StructuredEventFilter;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;

@ApplicationPath(CoreApi.API_ROOT_CONTEXT)
@Component
public class EndpointConfig extends ResourceConfig {
    private static final String VERSION_UNAVAILABLE = "unspecified";

    @Value("${info.app.version:}")
    private String cbVersion;

    @Value("${cb.structuredevent.rest.enabled:false}")
    private Boolean auditEnabled;

    @Inject
    private List<ExceptionMapper<?>> exceptionMappers;

    @PostConstruct
    private void init() {
        if (auditEnabled) {
            register(StructuredEventFilter.class);
        }
        registerEndpoints();
        registerExceptionMappers();
    }

    @PostConstruct
    private void registerSwagger() throws IOException {
        BeanConfig swaggerConfig = new BeanConfig();
        swaggerConfig.setTitle("Cloudbreak API");
        swaggerConfig.setDescription(FileReaderUtils.readFileFromClasspath("swagger/cloudbreak-introduction"));
        if (Strings.isNullOrEmpty(cbVersion)) {
            swaggerConfig.setVersion(VERSION_UNAVAILABLE);
        } else {
            swaggerConfig.setVersion(cbVersion);
        }
        swaggerConfig.setSchemes(new String[]{"http", "https"});
        swaggerConfig.setBasePath(CoreApi.API_ROOT_CONTEXT);
        swaggerConfig.setLicenseUrl("https://github.com/sequenceiq/cloudbreak/blob/master/LICENSE");
        swaggerConfig.setResourcePackage("com.sequenceiq.cloudbreak.api");
        swaggerConfig.setScan(true);
        swaggerConfig.setContact("https://hortonworks.com/contact-sales/");
        swaggerConfig.setPrettyPrint(true);
        SwaggerConfigLocator.getInstance().putConfig(SwaggerContextService.CONFIG_ID_DEFAULT, swaggerConfig);
    }

    private void registerExceptionMappers() {
        for (ExceptionMapper<?> mapper : exceptionMappers) {
            register(mapper);
        }
        register(WebApplicaitonExceptionMapper.class);
        register(DefaultExceptionMapper.class);
    }

    private void registerEndpoints() {
        register(AccountPreferencesController.class);
        register(AuditController.class);
        register(BlueprintController.class);
        register(BlueprintV3Controller.class);
        register(CloudbreakEventController.class);
        register(CloudbreakUsageController.class);
        register(ClusterV1Controller.class);
        register(CredentialController.class);
        register(CredentialV3Controller.class);
        register(FlexSubscriptionController.class);
        register(FlexSubscriptionV3Controller.class);
        register(ImageCatalogV1Controller.class);
        register(LdapController.class);
        register(LdapV3Controller.class);
        register(ManagementPackController.class);
        register(OrganizationV3Controller.class);
        register(PlatformParameterV1Controller.class);
        register(PlatformParameterV2Controller.class);
        register(ProxyConfigController.class);
        register(ProxyConfigV3Controller.class);
        register(RdsConfigController.class);
        register(RecipeController.class);
        register(RecipeV3Controller.class);
        register(RepositoryConfigValidationController.class);
        register(SecurityRuleController.class);
        register(SettingsController.class);
        register(SmartSenseSubscriptionController.class);
        register(SmartSenseSubscriptionV3Controller.class);
        register(StackV1Controller.class);
        register(StackV2Controller.class);
        register(StackV3Controller.class);
        register(SubscriptionController.class);
        register(UserController.class);
        register(UtilController.class);
        register(ManagementPackV3Controller.class);
        register(ImageCatalogV3Controller.class);
        register(RdsConfigV3Controller.class);
        register(AuditV3Controller.class);

        register(io.swagger.jaxrs.listing.ApiListingResource.class);
        register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
        register(io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.class);
    }
}
