package com.sequenceiq.cloudbreak.controller;

import java.io.IOException;

import javax.annotation.PostConstruct;
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
import com.sequenceiq.cloudbreak.controller.mapper.HibernateConstraintViolationExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.HttpMediaTypeNotSupportedExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.HttpMessageNotReadableExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.HttpRequestMethodNotSupportedExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.MethodArgumentNotValidExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.NotFoundExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.RuntimeExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.SmartSenseNotFoundExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.SpringAccessDeniedExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.SpringBadRequestExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.SubscriptionAlreadyExistExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.TerminationFailedExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.TypeMismatchExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.UnsupportedOperationFailedExceptionMapper;
import com.sequenceiq.cloudbreak.controller.mapper.WebApplicaitonExceptionMapper;
import com.sequenceiq.cloudbreak.filter.MDCContextFilter;
import com.sequenceiq.cloudbreak.structuredevent.rest.StructuredEventFilter;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;

@ApplicationPath(CoreApi.API_ROOT_CONTEXT)
@Component
public class EndpointConfig extends ResourceConfig {
    private static final String VERSION_UNAVAILABLE = "unspecified";

    @Value("${info.app.version:}")
    private String cbVersion;

    @Value("${cb.structuredevent.rest.enabled:false}")
    private Boolean auditEnabled;

    public EndpointConfig() {
    }

    @PostConstruct
    private void init() {
        if (auditEnabled) {
            register(StructuredEventFilter.class);
        }
        registerEndpoints();
        registerExceptionMappers();
        register(MDCContextFilter.class);
    }

    @PostConstruct
    private void registerSwagger() throws IOException {
        BeanConfig swaggerConfig = new BeanConfig();
        swaggerConfig.setTitle("Cloudbreak API");
        swaggerConfig.setDescription(FileReaderUtils.readFileFromClasspath("swagger/cloudbreak-introduction"));
        if (Strings.isNullOrEmpty(cbVersion)) {
            swaggerConfig.setVersion(VERSION_UNAVAILABLE);
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
        register(SmartSenseNotFoundExceptionMapper.class);
        register(SpringAccessDeniedExceptionMapper.class);
        register(SpringBadRequestExceptionMapper.class);
        register(SubscriptionAlreadyExistExceptionMapper.class);
        register(TypeMismatchExceptionMapper.class);
        register(UnsupportedOperationFailedExceptionMapper.class);
        register(HibernateConstraintViolationExceptionMapper.class);
        register(DataIntegrityViolationExceptionMapper.class);
        register(TerminationFailedExceptionMapper.class);
        register(WebApplicaitonExceptionMapper.class);

        register(RuntimeExceptionMapper.class);
        register(DefaultExceptionMapper.class);
    }

    private void registerEndpoints() {
        register(BlueprintController.class);
        register(PlatformParameterV1Controller.class);
        register(PlatformParameterV2Controller.class);
        register(ClusterV1Controller.class);
        register(ClusterV2Controller.class);
        register(CredentialController.class);
        register(NetworkController.class);
        register(RecipeController.class);
        register(SecurityGroupController.class);
        register(StackV1Controller.class);
        register(StackV2Controller.class);
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
        register(SmartSenseSubscriptionController.class);
        register(FlexSubscriptionController.class);

        register(io.swagger.jaxrs.listing.ApiListingResource.class);
        register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
        register(io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.class);
    }
}
