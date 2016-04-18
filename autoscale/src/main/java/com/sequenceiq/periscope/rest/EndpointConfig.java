package com.sequenceiq.periscope.rest;

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

@ApplicationPath(AutoscaleApi.API_ROOT_CONTEXT)
//TODO find a working solution for storing response codes globally
//@ApiResponses(value = {
//        @ApiResponse(code = HttpStatus.SC_OK, message = "Resource retrieved successfully"),
//        @ApiResponse(code = HttpStatus.SC_CREATED, message = "Resource created successfully"),
//        @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = "Resource request validation error"),
//        @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = "Unauthorized. Cannot access resource"),
//        @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = "Forbidden. Cannot access resource"),
//        @ApiResponse(code = HttpStatus.SC_NOT_ACCEPTABLE, message = "Media type is not acceptable"),
//        @ApiResponse(code = HttpStatus.SC_CONFLICT, message = "Resource updated successfully"),
//        @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = "Internal server error")
//})
@Component
public class EndpointConfig  extends ResourceConfig {

    public EndpointConfig() {
        registerEndpoints();
        registerExceptionMappers();
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
    }
}
