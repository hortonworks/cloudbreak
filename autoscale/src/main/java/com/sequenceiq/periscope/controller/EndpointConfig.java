package com.sequenceiq.periscope.controller;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.AutoscaleApi;
import com.sequenceiq.periscope.controller.mapper.AccessDeniedExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.BadRequestExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.ConstraintViolationExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.ConversionExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.ConversionFailedExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.DataIntegrityViolationExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.DefaultExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.HttpMediaTypeNotSupportedExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.HttpMessageNotReadableExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.HttpRequestMethodNotSupportedExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.JaxRsNotFoundExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.NotFoundExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.ParseExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.RuntimeExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.SpringAccessDeniedExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.SpringBadRequestExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.TypeMismatchExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.UnsupportedOperationFailedExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.WebApplicaitonExceptionMapper;
import com.sequenceiq.periscope.utils.FileReaderUtils;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;

@ApplicationPath(AutoscaleApi.API_ROOT_CONTEXT)
@Component
public class EndpointConfig extends ResourceConfig {

    @Value("${info.app.version:unspecified}")
    private String applicationVersion;

    @PostConstruct
    public void init() throws IOException {
        registerEndpoints();
        registerExceptionMappers();
        registerSwagger();
    }

    private void registerSwagger() throws IOException {
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("Auto-scaling API");
        beanConfig.setDescription(FileReaderUtils.readFileFromClasspath("swagger/auto-scaling-introduction"));
        beanConfig.setVersion(applicationVersion);
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
        register(ConversionExceptionMapper.class);
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
        register(ConstraintViolationExceptionMapper.class);
        register(JaxRsNotFoundExceptionMapper.class);

        register(RuntimeExceptionMapper.class);
        register(DefaultExceptionMapper.class);
    }

    private void registerEndpoints() {
        register(AlertController.class);
        register(DistroXAutoScaleClusterV1Controller.class);

        register(io.swagger.jaxrs.listing.ApiListingResource.class);
        register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
        register(io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.class);
    }
}
