package com.sequenceiq.redbeams.configuration;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.authorization.controller.AuthorizationInfoController;
import com.sequenceiq.cloudbreak.exception.mapper.DefaultExceptionMapper;
import com.sequenceiq.cloudbreak.structuredevent.rest.filter.CDPRestAuditFilter;
import com.sequenceiq.flow.controller.FlowController;
import com.sequenceiq.flow.controller.FlowPublicController;
import com.sequenceiq.redbeams.api.RedbeamsApi;
import com.sequenceiq.redbeams.controller.mapper.WebApplicationExceptionMapper;
import com.sequenceiq.redbeams.controller.v4.database.DatabaseV4Controller;
import com.sequenceiq.redbeams.controller.v4.databaseserver.DatabaseServerV4Controller;
import com.sequenceiq.redbeams.controller.v4.operation.OperationV4Controller;
import com.sequenceiq.redbeams.controller.v4.progress.ProgressV4Controller;
import com.sequenceiq.redbeams.controller.v4.support.SupportController;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

@ApplicationPath(RedbeamsApi.API_ROOT_CONTEXT)
@Configuration
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = List.of(
            DatabaseV4Controller.class,
            DatabaseServerV4Controller.class,
            ProgressV4Controller.class,
            OperationV4Controller.class,
            FlowController.class,
            FlowPublicController.class,
            AuthorizationInfoController.class,
            SupportController.class
    );

    @Value("${redbeams.structuredevent.rest.enabled:false}")
    private Boolean auditEnabled;

    @Inject
    private List<ExceptionMapper<?>> exceptionMappers;

    @PostConstruct
    private void init() {
        registerFilters();
        registerEndpoints();
        registerExceptionMappers();
        registerOpenApi();
    }

    private void registerOpenApi() {
        OpenApiResource openApiResource = new OpenApiResource();
        register(openApiResource);
    }

    private void registerExceptionMappers() {
        for (ExceptionMapper<?> mapper : exceptionMappers) {
            register(mapper);
        }
        register(WebApplicationExceptionMapper.class);
        register(DefaultExceptionMapper.class);
    }

    private void registerEndpoints() {
        CONTROLLERS.forEach(this::register);
    }

    private void registerFilters() {
        register(CDPRestAuditFilter.class);
    }
}
