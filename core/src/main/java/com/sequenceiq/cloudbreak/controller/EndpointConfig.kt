package com.sequenceiq.cloudbreak.controller

import javax.ws.rs.ApplicationPath

import org.glassfish.jersey.server.ResourceConfig
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.CoreApi
import com.sequenceiq.cloudbreak.controller.mapper.AccessDeniedExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.AuthenticationCredentialsNotFoundExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.BadRequestExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.ConstraintViolationExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.ConversionFailedExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.DataIntegrityViolationExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.DefaultExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.DuplicatedKeyValueExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.EntityNotFoundExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.HibernateConstraintViolationException
import com.sequenceiq.cloudbreak.controller.mapper.HttpMediaTypeNotSupportedExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.HttpMessageNotReadableExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.HttpRequestMethodNotSupportedExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.MethodArgumentNotValidExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.NotFoundExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.RuntimeExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.SpringAccessDeniedExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.SpringBadRequestExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.SubscriptionAlreadyExistExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.TerminationFailedExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.TypeMismatchExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.UnsupportedOperationFailedExceptionMapper
import com.sequenceiq.cloudbreak.controller.mapper.WebApplicaitonExceptionMapper

@ApplicationPath(CoreApi.API_ROOT_CONTEXT)
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
        register(AuthenticationCredentialsNotFoundExceptionMapper::class.java)
        register(BadRequestExceptionMapper::class.java)
        register(ConstraintViolationExceptionMapper::class.java)
        register(ConversionFailedExceptionMapper::class.java)
        register(DuplicatedKeyValueExceptionMapper::class.java)
        register(EntityNotFoundExceptionMapper::class.java)
        register(HttpMediaTypeNotSupportedExceptionMapper::class.java)
        register(HttpMessageNotReadableExceptionMapper::class.java)
        register(HttpRequestMethodNotSupportedExceptionMapper::class.java)
        register(MethodArgumentNotValidExceptionMapper::class.java)
        register(NotFoundExceptionMapper::class.java)
        register(SpringAccessDeniedExceptionMapper::class.java)
        register(SpringBadRequestExceptionMapper::class.java)
        register(SubscriptionAlreadyExistExceptionMapper::class.java)
        register(TypeMismatchExceptionMapper::class.java)
        register(UnsupportedOperationFailedExceptionMapper::class.java)
        register(HibernateConstraintViolationException::class.java)
        register(DataIntegrityViolationExceptionMapper::class.java)
        register(TerminationFailedExceptionMapper::class.java)
        register(WebApplicaitonExceptionMapper::class.java)

        register(RuntimeExceptionMapper::class.java)
        register(DefaultExceptionMapper::class.java)
    }

    private fun registerEndpoints() {
        register(BlueprintController::class.java)
        register(PlatformParameterController::class.java)
        register(ClusterController::class.java)
        register(CredentialController::class.java)
        register(NetworkController::class.java)
        register(RecipeController::class.java)
        register(SssdConfigController::class.java)
        register(SecurityGroupController::class.java)
        register(StackController::class.java)
        register(TemplateController::class.java)
        register(ConstraintTemplateController::class.java)
        register(UserController::class.java)
        register(TopologyController::class.java)
        register(ClusterTemplateController::class.java)

        register(CloudbreakEventController::class.java)
        register(SubscriptionController::class.java)
        register(CloudbreakUsageController::class.java)
        register(AccountPreferencesController::class.java)
        register(SettingsController::class.java)
        register(UtilController::class.java)

        register(io.swagger.jaxrs.listing.ApiListingResource::class.java)
        register(io.swagger.jaxrs.listing.SwaggerSerializers::class.java)
    }
}
