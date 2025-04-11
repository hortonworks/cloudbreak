package com.sequenceiq.periscope.config;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.controller.AuthorizationInfoController;
import com.sequenceiq.cloudbreak.controller.mapper.ForbiddenExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.NotFoundExceptionMapper;
import com.sequenceiq.cloudbreak.service.openapi.OpenApiController;
import com.sequenceiq.cloudbreak.service.openapi.OpenApiProvider;
import com.sequenceiq.periscope.api.AutoscaleApi;
import com.sequenceiq.periscope.controller.DistroXAutoScaleClusterV1Controller;
import com.sequenceiq.periscope.controller.DistroXAutoScaleScalingActivityV1Controller;
import com.sequenceiq.periscope.controller.DistroXAutoScaleYarnRecommendationV1Controller;
import com.sequenceiq.periscope.controller.HistoryController;
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
import com.sequenceiq.periscope.controller.mapper.ParseExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.RuntimeExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.SpringAccessDeniedExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.TypeMismatchExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.UnsupportedOperationFailedExceptionMapper;
import com.sequenceiq.periscope.controller.mapper.WebApplicaitonExceptionMapper;
import com.sequenceiq.periscope.utils.FileReaderUtils;

import io.swagger.v3.oas.models.OpenAPI;

@ApplicationPath(AutoscaleApi.API_ROOT_CONTEXT)
@Component
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = List.of(
            DistroXAutoScaleClusterV1Controller.class,
            DistroXAutoScaleScalingActivityV1Controller.class,
            DistroXAutoScaleYarnRecommendationV1Controller.class,
            HistoryController.class,
            AuthorizationInfoController.class,
            OpenApiController.class);

    @Value("${info.app.version:unspecified}")
    private String applicationVersion;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Inject
    private OpenApiProvider openApiProvider;

    @PostConstruct
    public void init() throws IOException {
        registerEndpoints();
        registerExceptionMappers();
        registerSwagger();
    }

    private void registerSwagger() throws IOException {
        OpenAPI openAPI = openApiProvider.getOpenAPI(
                "Auto-scaling API",
                FileReaderUtils.readFileFromClasspath("swagger/auto-scaling-introduction"),
                applicationVersion,
                "https://localhost" + contextPath + AutoscaleApi.API_ROOT_CONTEXT
        );
        openApiProvider.createConfig(openAPI, CONTROLLERS.stream().map(Class::getName).collect(Collectors.toSet()));
    }

    private void registerExceptionMappers() {
        register(ForbiddenExceptionMapper.class);
        register(BadRequestExceptionMapper.class);
        register(ConversionExceptionMapper.class);
        register(ConversionFailedExceptionMapper.class);
        register(HttpMediaTypeNotSupportedExceptionMapper.class);
        register(HttpMessageNotReadableExceptionMapper.class);
        register(HttpRequestMethodNotSupportedExceptionMapper.class);
        register(NotFoundExceptionMapper.class);
        register(SpringAccessDeniedExceptionMapper.class);
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
        CONTROLLERS.forEach(this::register);
    }
}
