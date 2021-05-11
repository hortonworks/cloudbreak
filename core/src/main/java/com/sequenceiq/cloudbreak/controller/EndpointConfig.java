package com.sequenceiq.cloudbreak.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.ExceptionMapper;

import com.sequenceiq.cloudbreak.controller.v4.CustomImageCatalogV4Controller;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.authorization.controller.AuthorizationInfoController;
import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.controller.mapper.DefaultExceptionMapper;
import com.sequenceiq.cloudbreak.controller.v4.AuditEventV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.AutoscaleV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.BlueprintUtilV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.BlueprintV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.CloudProviderServicesV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.CloudbreakInfoV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.ClusterTemplateV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.DatabaseConfigV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.DatabaseV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.DatalakeV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.DiagnosticsV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.EventV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.FileSystemV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.ImageCatalogV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.ProgressV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.RecipesV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.StackV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.UserProfileV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.UtilV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.WorkspaceAwareUtilV4Controller;
import com.sequenceiq.cloudbreak.structuredevent.rest.LegacyStructuredEventFilter;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.distrox.v1.distrox.controller.DistroXDatabaseServerV1Controller;
import com.sequenceiq.distrox.v1.distrox.controller.DistroXInternalV1Controller;
import com.sequenceiq.distrox.v1.distrox.controller.DistroXV1Controller;
import com.sequenceiq.distrox.v1.distrox.controller.DistroXUpgradeV1Controller;
import com.sequenceiq.flow.controller.FlowController;
import com.sequenceiq.flow.controller.FlowPublicController;

import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;

@ApplicationPath(CoreApi.API_ROOT_CONTEXT)
@Configuration
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = Arrays.asList(
            AuditEventV4Controller.class,
            BlueprintV4Controller.class,
            BlueprintUtilV4Controller.class,
            EventV4Controller.class,
            ClusterTemplateV4Controller.class,
            DatabaseV4Controller.class,
            DatabaseConfigV4Controller.class,
            ImageCatalogV4Controller.class,
            CustomImageCatalogV4Controller.class,
            RecipesV4Controller.class,
            UserProfileV4Controller.class,
            FileSystemV4Controller.class,
            UtilV4Controller.class,
            FileSystemV4Controller.class,
            WorkspaceAwareUtilV4Controller.class,
            AutoscaleV4Controller.class,
            StackV4Controller.class,
            CloudbreakInfoV4Controller.class,
            DistroXV1Controller.class,
            DistroXInternalV1Controller.class,
            DatalakeV4Controller.class,
            DiagnosticsV4Controller.class,
            ProgressV4Controller.class,
            CloudProviderServicesV4Controller.class,
            FlowController.class,
            FlowPublicController.class,
            AuthorizationInfoController.class,
            DistroXUpgradeV1Controller.class,
            DistroXDatabaseServerV1Controller.class,
            AuthorizationUtilEndpoint.class
    );

    @Value("${info.app.version:unspecified}")
    private String cbVersion;

    @Value("${cb.structuredevent.rest.enabled}")
    private Boolean auditEnabled;

    @Inject
    private List<ExceptionMapper<?>> exceptionMappers;

    @Inject
    private ServerTracingDynamicFeature serverTracingDynamicFeature;

    @Inject
    private ClientTracingFeature clientTracingFeature;

    @PostConstruct
    private void init() {
        if (auditEnabled) {
            register(LegacyStructuredEventFilter.class);
        }
        registerEndpoints();
        registerExceptionMappers();
        register(serverTracingDynamicFeature);
        register(clientTracingFeature);
    }

    @PostConstruct
    private void registerSwagger() throws IOException {
        BeanConfig swaggerConfig = new BeanConfig();
        swaggerConfig.setTitle("Cloudbreak API");
        swaggerConfig.setDescription(FileReaderUtils.readFileFromClasspath("swagger/cloudbreak-introduction"));
        swaggerConfig.setVersion(cbVersion);
        swaggerConfig.setSchemes(new String[]{"http", "https"});
        swaggerConfig.setBasePath(CoreApi.API_ROOT_CONTEXT);
        swaggerConfig.setLicenseUrl("https://github.com/hortonworks/cloudbreak/blob/master/LICENSE");
        swaggerConfig.setResourcePackage("com.sequenceiq.cloudbreak.api,com.sequenceiq.distrox.api,com.sequenceiq.flow.api,com.sequenceiq.authorization");
        swaggerConfig.setScan(true);
        swaggerConfig.setContact("https://hortonworks.com/contact-sales/");
        swaggerConfig.setPrettyPrint(true);
        SwaggerConfigLocator.getInstance().putConfig(SwaggerContextService.CONFIG_ID_DEFAULT, swaggerConfig);
    }

    private void registerExceptionMappers() {
        for (ExceptionMapper<?> mapper : exceptionMappers) {
            register(mapper);
        }
        register(DefaultExceptionMapper.class);
    }

    private void registerEndpoints() {
        CONTROLLERS.forEach(this::register);

        register(io.swagger.jaxrs.listing.ApiListingResource.class);
        register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
        register(io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.class);
    }
}
