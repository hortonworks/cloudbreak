package com.sequenceiq.datalake.configuration;

import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.authorization.controller.AuthorizationInfoController;
import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.cloudbreak.structuredevent.rest.controller.CDPStructuredEventV1Controller;
import com.sequenceiq.datalake.controller.SdxEventController;
import com.sequenceiq.datalake.controller.diagnostics.DiagnosticsController;
import com.sequenceiq.datalake.controller.mapper.DefaultExceptionMapper;
import com.sequenceiq.datalake.controller.mapper.WebApplicaitonExceptionMapper;
import com.sequenceiq.datalake.controller.operation.OperationController;
import com.sequenceiq.datalake.controller.progress.ProgressController;
import com.sequenceiq.datalake.controller.sdx.DatabaseConfigController;
import com.sequenceiq.datalake.controller.sdx.DatabaseServerController;
import com.sequenceiq.datalake.controller.sdx.SdxBackupController;
import com.sequenceiq.datalake.controller.sdx.SdxController;
import com.sequenceiq.datalake.controller.sdx.SdxInternalController;
import com.sequenceiq.datalake.controller.sdx.SdxRestoreController;
import com.sequenceiq.datalake.controller.sdx.SdxUpgradeController;
import com.sequenceiq.datalake.controller.util.UtilController;
import com.sequenceiq.flow.controller.FlowController;
import com.sequenceiq.flow.controller.FlowPublicController;
import com.sequenceiq.sdx.api.SdxApi;

import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;

@ApplicationPath(SdxApi.API_ROOT_CONTEXT)
@Configuration
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = Arrays.asList(
            SdxController.class,
            SdxUpgradeController.class,
            SdxInternalController.class,
            DatabaseConfigController.class,
            UtilController.class,
            FlowController.class,
            FlowPublicController.class,
            AuthorizationInfoController.class,
            DiagnosticsController.class,
            ProgressController.class,
            OperationController.class,
            AuthorizationUtilEndpoint.class,
            DatabaseServerController.class,
            SdxBackupController.class,
            SdxRestoreController.class,
            CDPStructuredEventV1Controller.class,
            SdxEventController.class
    );

    @Value("${info.app.version:unspecified}")
    private String applicationVersion;

    @Value("${datalake.structuredevent.rest.enabled:false}")
    private Boolean auditEnabled;

    @Inject
    private List<ExceptionMapper<?>> exceptionMappers;

    @Inject
    private ServerTracingDynamicFeature serverTracingDynamicFeature;

    @Inject
    private ClientTracingFeature clientTracingFeature;

    @PostConstruct
    private void init() {
        registerEndpoints();
        registerExceptionMappers();
        register(serverTracingDynamicFeature);
        register(clientTracingFeature);
    }

    @PostConstruct
    private void registerSwagger() {
        BeanConfig swaggerConfig = new BeanConfig();
        swaggerConfig.setTitle("Datalake API");
        swaggerConfig.setDescription("");
        swaggerConfig.setVersion(applicationVersion);
        swaggerConfig.setSchemes(new String[]{"http", "https"});
        swaggerConfig.setBasePath(SdxApi.API_ROOT_CONTEXT);
        swaggerConfig.setLicenseUrl("https://github.com/sequenceiq/cloudbreak/blob/master/LICENSE");
        swaggerConfig.setResourcePackage("com.sequenceiq.sdx.api,com.sequenceiq.flow.api,com.sequenceiq.authorization");
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
