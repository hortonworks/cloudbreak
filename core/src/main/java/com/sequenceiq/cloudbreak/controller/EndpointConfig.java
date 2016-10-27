package com.sequenceiq.cloudbreak.controller;

import java.io.IOException;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.repackaged.com.google.common.base.Strings;
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
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;

@ApplicationPath(CoreApi.API_ROOT_CONTEXT)
//TODO find a working solution for storing response codes globally
@Component
public class EndpointConfig extends ResourceConfig {

    @Value("${info.app.version:}")
    private String cbVersion;

    public EndpointConfig() throws IOException {
        registerEndpoints();
        registerExceptionMappers();
        registerSwagger();
    }

    private void registerSwagger() throws IOException {
        BeanConfig swaggerConfig = new BeanConfig();
        swaggerConfig.setTitle("Cloudbreak API");
        swaggerConfig.setDescription(FileReaderUtils.readFileFromClasspath("swagger/cloudbreak-introduction"));
        if (Strings.isNullOrEmpty(cbVersion)) {
            swaggerConfig.setVersion("1.9.0");
        } else {
            swaggerConfig.setVersion(cbVersion);
        }
        swaggerConfig.setSchemes(new String[]{"http", "https"});
        swaggerConfig.setBasePath(CoreApi.API_ROOT_CONTEXT);
        swaggerConfig.setLicenseUrl("https://github.com/sequenceiq/cloudbreak/blob/master/LICENSE");
        swaggerConfig.setResourcePackage("com.sequenceiq.cloudbreak.api");
        swaggerConfig.setScan(true);
        swaggerConfig.setContact("https://hortonworks.com/contact-sales/");
        swaggerConfig.setPrettyPrint(true);
        SwaggerConfigLocator.getInstance().putConfig(SwaggerContextService.CONFIG_ID_DEFAULT, swaggerConfig);
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
        register(LdapController.class);

        register(io.swagger.jaxrs.listing.ApiListingResource.class);
        register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
        register(io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.class);

    }
}
