package com.sequenceiq.redbeams.configuration;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
// import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.redbeams.api.RedbeamsApi;
import com.sequenceiq.redbeams.controller.mapper.DefaultExceptionMapper;
import com.sequenceiq.redbeams.controller.mapper.WebApplicationExceptionMapper;
import com.sequenceiq.redbeams.controller.v4.database.DatabaseV4Controller;
import com.sequenceiq.redbeams.controller.v4.databaseserver.DatabaseServerV4Controller;

import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;

@ApplicationPath(RedbeamsApi.API_ROOT_CONTEXT)
@Configuration
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = List.of(
        DatabaseV4Controller.class,
        DatabaseServerV4Controller.class
    );

    // @Value("${redbeams.structuredevent.rest.enabled:false}")
    // private Boolean auditEnabled;

    @Inject
    private List<ExceptionMapper<?>> exceptionMappers;

    @Inject
    private ServerTracingDynamicFeature serverTracingDynamicFeature;

    @Inject
    private ClientTracingFeature clientTracingFeature;

    @PostConstruct
    private void init() {
        /* TODO Add StructuredEventFilter, preferably as a library
            if (auditEnabled) {
                register(StructuredEventFilter.class);
            }
         */
        registerEndpoints();
        registerExceptionMappers();
        register(serverTracingDynamicFeature);
        register(clientTracingFeature);
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

        register(io.swagger.jaxrs.listing.ApiListingResource.class);
        register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
        register(io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.class);
    }
}
