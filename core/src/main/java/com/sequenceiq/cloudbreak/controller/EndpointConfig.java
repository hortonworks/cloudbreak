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
import com.sequenceiq.cloudbreak.controller.mapper.DefaultExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.WebApplicaitonExceptionMapper;
import com.sequenceiq.cloudbreak.controller.v4.AuditEventV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.AutoscaleV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.BlueprintUtilV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.BlueprintV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.CloudProviderServicesV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.CloudbreakInfoV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.ClusterTemplateV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.DatabaseV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.DatalakeV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.EventV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.FileSystemV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.ImageCatalogV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.KubernetesV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.ManagementPackV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.RecipesV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.StackV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.UserProfileV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.UserV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.UtilV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.WorkspaceAwareUtilV4Controller;
import com.sequenceiq.cloudbreak.controller.v4.WorkspaceV4Controller;
import com.sequenceiq.cloudbreak.structuredevent.rest.StructuredEventFilter;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.distrox.v1.distrox.controller.DistroXV1Controller;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;

@ApplicationPath(CoreApi.API_ROOT_CONTEXT)
@Controller
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = Arrays.asList(
            AuditEventV4Controller.class,
            BlueprintV4Controller.class,
            BlueprintUtilV4Controller.class,
            EventV4Controller.class,
            ClusterTemplateV4Controller.class,
            DatabaseV4Controller.class,
            ImageCatalogV4Controller.class,
            KubernetesV4Controller.class,
            WorkspaceV4Controller.class,
            RecipesV4Controller.class,
            UserProfileV4Controller.class,
            UserV4Controller.class,
            FileSystemV4Controller.class,
            UtilV4Controller.class,
            FileSystemV4Controller.class,
            WorkspaceAwareUtilV4Controller.class,
            AutoscaleV4Controller.class,
            ManagementPackV4Controller.class,
            StackV4Controller.class,
            CloudbreakInfoV4Controller.class,
            DistroXV1Controller.class,
            DatalakeV4Controller.class,
            CloudProviderServicesV4Controller.class
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
        swaggerConfig.setLicenseUrl("https://github.com/hortonworks/cloudbreak/blob/master/LICENSE");
        swaggerConfig.setResourcePackage("com.sequenceiq.cloudbreak.api,com.sequenceiq.distrox.api");
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
