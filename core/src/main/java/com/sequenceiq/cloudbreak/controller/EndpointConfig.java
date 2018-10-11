package com.sequenceiq.cloudbreak.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.controller.audit.AuditController;
import com.sequenceiq.cloudbreak.controller.audit.AuditV3Controller;
import com.sequenceiq.cloudbreak.controller.mapper.DefaultExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.WebApplicaitonExceptionMapper;
import com.sequenceiq.cloudbreak.structuredevent.rest.StructuredEventFilter;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;

@ApplicationPath(CoreApi.API_ROOT_CONTEXT)
@Controller
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS  = Arrays.asList(
        AccountPreferencesController.class,
        AuditController.class,
        AuditV3Controller.class,
        BlueprintController.class,
        BlueprintV3Controller.class,
        CloudbreakEventController.class,
        CloudbreakEventV3Controller.class,
        CloudbreakUsageController.class,
        ClusterV1Controller.class,
        ClusterTemplateV3Controller.class,
        CredentialController.class,
        CredentialV3Controller.class,
        EnvironmentV3Controller.class,
        FlexSubscriptionController.class,
        FlexSubscriptionV3Controller.class,
        ImageCatalogV1Controller.class,
        ImageCatalogV3Controller.class,
        KnoxServicesV3Controller.class,
        LdapController.class,
        LdapV3Controller.class,
        ManagementPackController.class,
        ManagementPackV3Controller.class,
        WorkspaceV3Controller.class,
        PlatformParameterV1Controller.class,
        PlatformParameterV2Controller.class,
        PlatformParameterV3Controller.class,
        ProxyConfigController.class,
        ProxyConfigV3Controller.class,
        RdsConfigController.class,
        RdsConfigV3Controller.class,
        RecipeController.class,
        RecipeV3Controller.class,
        RepositoryConfigValidationController.class,
        SecurityRuleController.class,
        SettingsController.class,
        SmartSenseSubscriptionController.class,
        SmartSenseSubscriptionV3Controller.class,
        StackV1Controller.class,
        StackV2Controller.class,
        StackV3Controller.class,
        SubscriptionController.class,
        UserController.class,
        UserV3Controller.class,
        UtilController.class,
        UtilV3Controller.class,
        FileSystemV3Controller.class,
        AutoscaleController.class
    );

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
        CONTROLLERS.forEach(this::register);

        register(io.swagger.jaxrs.listing.ApiListingResource.class);
        register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
        register(io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.class);
    }
}
