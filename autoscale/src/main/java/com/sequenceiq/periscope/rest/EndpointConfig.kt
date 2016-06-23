package com.sequenceiq.periscope.rest

import javax.ws.rs.ApplicationPath

import org.glassfish.jersey.server.ResourceConfig
import org.springframework.stereotype.Component

import com.sequenceiq.periscope.api.AutoscaleApi
import com.sequenceiq.periscope.rest.controller.AlertController
import com.sequenceiq.periscope.rest.controller.ClusterController
import com.sequenceiq.periscope.rest.controller.ConfigurationController
import com.sequenceiq.periscope.rest.controller.HistoryController
import com.sequenceiq.periscope.rest.controller.PolicyController
import com.sequenceiq.periscope.rest.mapper.AccessDeniedExceptionMapper
import com.sequenceiq.periscope.rest.mapper.BadRequestExceptionMapper
import com.sequenceiq.periscope.rest.mapper.ConversionFailedExceptionMapper
import com.sequenceiq.periscope.rest.mapper.DataIntegrityViolationExceptionMapper
import com.sequenceiq.periscope.rest.mapper.DefaultExceptionMapper
import com.sequenceiq.periscope.rest.mapper.HttpMediaTypeNotSupportedExceptionMapper
import com.sequenceiq.periscope.rest.mapper.HttpMessageNotReadableExceptionMapper
import com.sequenceiq.periscope.rest.mapper.HttpRequestMethodNotSupportedExceptionMapper
import com.sequenceiq.periscope.rest.mapper.NotFoundExceptionMapper
import com.sequenceiq.periscope.rest.mapper.ParseExceptionMapper
import com.sequenceiq.periscope.rest.mapper.RuntimeExceptionMapper
import com.sequenceiq.periscope.rest.mapper.SpringAccessDeniedExceptionMapper
import com.sequenceiq.periscope.rest.mapper.SpringBadRequestExceptionMapper
import com.sequenceiq.periscope.rest.mapper.TypeMismatchExceptionMapper
import com.sequenceiq.periscope.rest.mapper.UnsupportedOperationFailedExceptionMapper
import com.sequenceiq.periscope.rest.mapper.WebApplicaitonExceptionMapper

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
class EndpointConfig : ResourceConfig() {

    init {
        registerEndpoints()
        registerExceptionMappers()
    }

    private fun registerExceptionMappers() {
        register(AccessDeniedExceptionMapper::class.java)
        register(BadRequestExceptionMapper::class.java)
        register(ConversionFailedExceptionMapper::class.java)
        register(HttpMediaTypeNotSupportedExceptionMapper::class.java)
        register(HttpMessageNotReadableExceptionMapper::class.java)
        register(HttpRequestMethodNotSupportedExceptionMapper::class.java)
        register(NotFoundExceptionMapper::class.java)
        register(SpringAccessDeniedExceptionMapper::class.java)
        register(SpringBadRequestExceptionMapper::class.java)
        register(TypeMismatchExceptionMapper::class.java)
        register(UnsupportedOperationFailedExceptionMapper::class.java)
        register(DataIntegrityViolationExceptionMapper::class.java)
        register(WebApplicaitonExceptionMapper::class.java)
        register(ParseExceptionMapper::class.java)

        register(RuntimeExceptionMapper::class.java)
        register(DefaultExceptionMapper::class.java)
    }

    private fun registerEndpoints() {
        register(AlertController::class.java)
        register(ClusterController::class.java)
        register(ConfigurationController::class.java)
        register(HistoryController::class.java)
        register(PolicyController::class.java)

        register(io.swagger.jaxrs.listing.ApiListingResource::class.java)
        register(io.swagger.jaxrs.listing.SwaggerSerializers::class.java)
    }
}
