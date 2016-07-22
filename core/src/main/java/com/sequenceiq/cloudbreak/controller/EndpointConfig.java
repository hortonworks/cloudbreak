package com.sequenceiq.cloudbreak.controller;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.controller.mapper.AccessDeniedExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.AuthenticationCredentialsNotFoundExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.BadRequestExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.CloudbreakApiExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.ConstraintViolationExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.ConversionFailedExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.DataIntegrityViolationExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.DefaultExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.DuplicatedKeyValueExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.EntityNotFoundExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.HibernateConstraintViolationException;
import com.sequenceiq.cloudbreak.controller.mapper.HttpMediaTypeNotSupportedExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.HttpMessageNotReadableExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.HttpRequestMethodNotSupportedExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.MethodArgumentNotValidExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.NotFoundExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.RuntimeExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.SpringAccessDeniedExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.SpringBadRequestExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.SubscriptionAlreadyExistExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.TerminationFailedExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.TypeMismatchExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.UnsupportedOperationFailedExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.WebApplicaitonExceptionMapper;

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
public class EndpointConfig extends ResourceConfig {

    public EndpointConfig() {
        registerEndpoints();
        registerExceptionMappers();
    }

    private void registerExceptionMappers() {
        register(AccessDeniedExceptionMapper.class);
        register(AuthenticationCredentialsNotFoundExceptionMapper.class);
        register(BadRequestExceptionMapper.class);
        register(CloudbreakApiExceptionMapper.class);
        register(ConstraintViolationExceptionMapper.class);
        register(ConversionFailedExceptionMapper.class);
        register(DuplicatedKeyValueExceptionMapper.class);
        register(EntityNotFoundExceptionMapper.class);
        register(HttpMediaTypeNotSupportedExceptionMapper.class);
        register(HttpMessageNotReadableExceptionMapper.class);
        register(HttpRequestMethodNotSupportedExceptionMapper.class);
        register(MethodArgumentNotValidExceptionMapper.class);
        register(NotFoundExceptionMapper.class);
        register(SpringAccessDeniedExceptionMapper.class);
        register(SpringBadRequestExceptionMapper.class);
        register(SubscriptionAlreadyExistExceptionMapper.class);
        register(TypeMismatchExceptionMapper.class);
        register(UnsupportedOperationFailedExceptionMapper.class);
        register(HibernateConstraintViolationException.class);
        register(DataIntegrityViolationExceptionMapper.class);
        register(TerminationFailedExceptionMapper.class);
        register(WebApplicaitonExceptionMapper.class);

        register(RuntimeExceptionMapper.class);
        register(DefaultExceptionMapper.class);
    }

    private void registerEndpoints() {
        register(BlueprintController.class);
        register(PlatformParameterController.class);
        register(ClusterController.class);
        register(CredentialController.class);
        register(NetworkController.class);
        register(RecipeController.class);
        register(SssdConfigController.class);
        register(SecurityGroupController.class);
        register(StackController.class);
        register(TemplateController.class);
        register(ConstraintTemplateController.class);
        register(UserController.class);
        register(TopologyController.class);
        register(ClusterTemplateController.class);

        register(CloudbreakEventController.class);
        register(SubscriptionController.class);
        register(CloudbreakUsageController.class);
        register(AccountPreferencesController.class);
        register(SettingsController.class);
        register(UtilController.class);
        register(RdsConfigController.class);

        register(io.swagger.jaxrs.listing.ApiListingResource.class);
        register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
    }
}
