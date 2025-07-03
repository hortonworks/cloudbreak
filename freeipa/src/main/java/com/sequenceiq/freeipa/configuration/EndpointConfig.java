package com.sequenceiq.freeipa.configuration;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.authorization.controller.AuthorizationInfoController;
import com.sequenceiq.authorization.controller.AuthorizationUtilController;
import com.sequenceiq.cloudbreak.exception.mapper.DefaultExceptionMapper;
import com.sequenceiq.cloudbreak.service.openapi.OpenApiController;
import com.sequenceiq.cloudbreak.service.openapi.OpenApiProvider;
import com.sequenceiq.cloudbreak.structuredevent.rest.controller.CDPStructuredEventV1Controller;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPEventV1Endpoint;
import com.sequenceiq.cloudbreak.structuredevent.rest.filter.CDPRestAuditFilter;
import com.sequenceiq.cloudbreak.structuredevent.rest.filter.CDPStructuredEventFilter;
import com.sequenceiq.flow.controller.FlowPublicController;
import com.sequenceiq.freeipa.api.FreeIpaApi;
import com.sequenceiq.freeipa.controller.ClientTestV1Controller;
import com.sequenceiq.freeipa.controller.DiagnosticsV1Controller;
import com.sequenceiq.freeipa.controller.DnsV1Controller;
import com.sequenceiq.freeipa.controller.EncryptionV1Controller;
import com.sequenceiq.freeipa.controller.FreeIpaCO2V1Controller;
import com.sequenceiq.freeipa.controller.FreeIpaCostV1Controller;
import com.sequenceiq.freeipa.controller.FreeIpaRotationV1Controller;
import com.sequenceiq.freeipa.controller.FreeIpaUpgradeV1Controller;
import com.sequenceiq.freeipa.controller.FreeIpaV1Controller;
import com.sequenceiq.freeipa.controller.FreeIpaV1FlowController;
import com.sequenceiq.freeipa.controller.FreeIpaV2Controller;
import com.sequenceiq.freeipa.controller.OperationV1Controller;
import com.sequenceiq.freeipa.controller.ProgressV1Controller;
import com.sequenceiq.freeipa.controller.RecipeV1Controller;
import com.sequenceiq.freeipa.controller.TrustV1Controller;
import com.sequenceiq.freeipa.controller.UserV1Controller;
import com.sequenceiq.freeipa.controller.UtilV1Controller;
import com.sequenceiq.freeipa.controller.mapper.WebApplicaitonExceptionMapper;
import com.sequenceiq.freeipa.kerberos.v1.KerberosConfigV1Controller;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KerberosMgmtV1Controller;
import com.sequenceiq.freeipa.ldap.v1.LdapConfigV1Controller;

import io.swagger.v3.oas.models.OpenAPI;

@ApplicationPath(FreeIpaApi.API_ROOT_CONTEXT)
@Configuration
public class EndpointConfig extends ResourceConfig {

    private static final List<Class<?>> CONTROLLERS = List.of(
            UserV1Controller.class,
            ClientTestV1Controller.class,
            FreeIpaV1Controller.class,
            FreeIpaV2Controller.class,
            LdapConfigV1Controller.class,
            KerberosConfigV1Controller.class,
            KerberosMgmtV1Controller.class,
            DnsV1Controller.class,
            OperationV1Controller.class,
            FreeIpaV1FlowController.class,
            FlowPublicController.class,
            AuthorizationInfoController.class,
            DiagnosticsV1Controller.class,
            ProgressV1Controller.class,
            CDPStructuredEventV1Controller.class,
            UtilV1Controller.class,
            FreeIpaUpgradeV1Controller.class,
            RecipeV1Controller.class,
            FreeIpaCostV1Controller.class,
            FreeIpaCO2V1Controller.class,
            FreeIpaRotationV1Controller.class,
            CDPEventV1Endpoint.class,
            AuthorizationUtilController.class,
            OpenApiController.class,
            EncryptionV1Controller.class,
            TrustV1Controller.class);

    @Value("${info.app.version:unspecified}")
    private String applicationVersion;

    @Value("${freeipa.structuredevent.rest.enabled:false}")
    private Boolean auditEnabled;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Inject
    private List<ExceptionMapper<?>> exceptionMappers;

    @Inject
    private OpenApiProvider openApiProvider;

    @PostConstruct
    private void init() {
        registerFilters();
        registerEndpoints();
        registerExceptionMappers();
        registerSwagger();
    }

    private void registerSwagger() {
        OpenAPI openAPI = openApiProvider.getOpenAPI(
                "FreeIPA API",
                "API for working with FreeIPA clusters",
                applicationVersion,
                "https://localhost" + contextPath + FreeIpaApi.API_ROOT_CONTEXT
                );
        openApiProvider.createConfig(openAPI, CONTROLLERS.stream().map(Class::getName).collect(Collectors.toSet()));
    }

    private void registerExceptionMappers() {
        for (ExceptionMapper<?> mapper : exceptionMappers) {
            register(mapper);
        }
        register(WebApplicaitonExceptionMapper.class);
        register(DefaultExceptionMapper.class);
    }

    private void registerEndpoints() {
        CONTROLLERS.forEach(this::register);
    }

    private void registerFilters() {
        register(CDPRestAuditFilter.class);
        if (auditEnabled) {
            register(CDPStructuredEventFilter.class);
        }
    }
}

