package com.sequenceiq.periscope.rest;

import java.io.IOException;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.AutoscaleApi;
import com.sequenceiq.periscope.rest.controller.AlertController;
import com.sequenceiq.periscope.rest.controller.ClusterController;
import com.sequenceiq.periscope.rest.controller.ConfigurationController;
import com.sequenceiq.periscope.rest.controller.HistoryController;
import com.sequenceiq.periscope.rest.controller.PolicyController;
import com.sequenceiq.periscope.rest.mapper.AccessDeniedExceptionMapper;
import com.sequenceiq.periscope.rest.mapper.BadRequestExceptionMapper;
import com.sequenceiq.periscope.rest.mapper.ConversionFailedExceptionMapper;
import com.sequenceiq.periscope.rest.mapper.DataIntegrityViolationExceptionMapper;
import com.sequenceiq.periscope.rest.mapper.DefaultExceptionMapper;
import com.sequenceiq.periscope.rest.mapper.HttpMediaTypeNotSupportedExceptionMapper;
import com.sequenceiq.periscope.rest.mapper.HttpMessageNotReadableExceptionMapper;
import com.sequenceiq.periscope.rest.mapper.HttpRequestMethodNotSupportedExceptionMapper;
import com.sequenceiq.periscope.rest.mapper.NotFoundExceptionMapper;
import com.sequenceiq.periscope.rest.mapper.ParseExceptionMapper;
import com.sequenceiq.periscope.rest.mapper.RuntimeExceptionMapper;
import com.sequenceiq.periscope.rest.mapper.SpringAccessDeniedExceptionMapper;
import com.sequenceiq.periscope.rest.mapper.SpringBadRequestExceptionMapper;
import com.sequenceiq.periscope.rest.mapper.TypeMismatchExceptionMapper;
import com.sequenceiq.periscope.rest.mapper.UnsupportedOperationFailedExceptionMapper;
import com.sequenceiq.periscope.rest.mapper.WebApplicaitonExceptionMapper;
import com.sequenceiq.periscope.utils.FileReaderUtils;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;

@ApplicationPath(AutoscaleApi.API_ROOT_CONTEXT)
@Component
public class EndpointConfig  extends ResourceConfig {

    public EndpointConfig() throws IOException {
        registerEndpoints();
        registerExceptionMappers();
        registerSwagger();
    }

    private void registerSwagger() throws IOException {
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("Auto-scaling API");
        beanConfig.setDescription(FileReaderUtils.readFileFromClasspath("swagger/auto-scaling-introduction"));
        beanConfig.setVersion("1.9.0");
        beanConfig.setSchemes(new String[]{"http", "https"});
        beanConfig.setBasePath(AutoscaleApi.API_ROOT_CONTEXT);
        beanConfig.setLicenseUrl("https://github.com/sequenceiq/cloudbreak/blob/master/LICENSE");
        beanConfig.setResourcePackage("com.sequenceiq.periscope.api");
        beanConfig.setScan(true);
        beanConfig.setContact("https://hortonworks.com/contact-sales/");
        beanConfig.setPrettyPrint(true);
        SwaggerConfigLocator.getInstance().putConfig(SwaggerContextService.CONFIG_ID_DEFAULT, beanConfig);
    }

    private void registerExceptionMappers() {
        register(AccessDeniedExceptionMapper.class);
        register(BadRequestExceptionMapper.class);
        register(ConversionFailedExceptionMapper.class);
        register(HttpMediaTypeNotSupportedExceptionMapper.class);
        register(HttpMessageNotReadableExceptionMapper.class);
        register(HttpRequestMethodNotSupportedExceptionMapper.class);
        register(NotFoundExceptionMapper.class);
        register(SpringAccessDeniedExceptionMapper.class);
        register(SpringBadRequestExceptionMapper.class);
        register(TypeMismatchExceptionMapper.class);
        register(UnsupportedOperationFailedExceptionMapper.class);
        register(DataIntegrityViolationExceptionMapper.class);
        register(WebApplicaitonExceptionMapper.class);
        register(ParseExceptionMapper.class);

        register(RuntimeExceptionMapper.class);
        register(DefaultExceptionMapper.class);
    }

    private void registerEndpoints() {
        register(AlertController.class);
        register(ClusterController.class);
        register(ConfigurationController.class);
        register(HistoryController.class);
        register(PolicyController.class);

        register(io.swagger.jaxrs.listing.ApiListingResource.class);
        register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
        register(io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.class);
    }
}
