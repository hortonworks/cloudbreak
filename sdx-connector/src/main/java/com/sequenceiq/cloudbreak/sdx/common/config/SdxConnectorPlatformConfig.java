package com.sequenceiq.cloudbreak.sdx.common.config;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.registry.ServiceAddressResolver;
import com.sequenceiq.cloudbreak.registry.ServiceAddressResolvingException;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDeleteService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDhTearDownService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxStartStopService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxStatusService;
import com.sequenceiq.environment.client.internal.EnvironmentApiClientParams;
import com.sequenceiq.remoteenvironment.api.client.internal.RemoteEnvironmentApiClientParams;

@Configuration
public class SdxConnectorPlatformConfig {

    @Inject
    private Optional<List<PlatformAwareSdxStatusService<?>>> platformDependentSdxStatusServices;

    @Inject
    private Optional<List<PlatformAwareSdxDeleteService<?>>> platformDependentSdxDeleteServices;

    @Inject
    private Optional<List<PlatformAwareSdxStartStopService>> platformDependentSdxStartStopServices;

    @Inject
    private Optional<List<PlatformAwareSdxDescribeService>> platformDependentSdxDescribeServices;

    @Inject
    private Optional<List<PlatformAwareSdxDhTearDownService>> platformDependentSdxDhTearDownServices;

    @Value("${cb.remoteEnvironment.url:http://localhost:8092}")
    private String remoteEnvironmentServiceUrl;

    @Value("${cb.remoteEnvironment.contextPath:/remoteenvironmentservice}")
    private String remoteEnvironmentContextPath;

    @Value("${cb.remoteEnvironment.serviceid:}")
    private String remoteEnvironmentServiceId;

    @Value("${cb.environment.url:http://localhost:8088}")
    private String environmentServiceUrl;

    @Value("${cb.environment.contextPath:/environmentservice}")
    private String environmentContextPath;

    @Value("${cb.environment.serviceid:}")
    private String environmentServiceId;

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:true}")
    private boolean ignorePreValidation;

    @Inject
    private ServiceAddressResolver serviceAddressResolver;

    @Bean
    public Map<TargetPlatform, PlatformAwareSdxStatusService<?>> platformDependentSdxStatusServicesMap() {
        if (platformDependentSdxStatusServices.isPresent()) {
            Map<TargetPlatform, PlatformAwareSdxStatusService<?>> bean = new EnumMap<>(TargetPlatform.class);
            for (PlatformAwareSdxStatusService<?> platformAwareSdxStatusService : platformDependentSdxStatusServices.get()) {
                bean.put(platformAwareSdxStatusService.targetPlatform(), platformAwareSdxStatusService);
            }
            return Maps.immutableEnumMap(bean);
        } else {
            return Map.of();
        }
    }

    @Bean
    public Map<TargetPlatform, PlatformAwareSdxDeleteService<?>> platformDependentSdxDeleteServicesMap() {
        if (platformDependentSdxDeleteServices.isPresent()) {
            Map<TargetPlatform, PlatformAwareSdxDeleteService<?>> bean = new EnumMap<>(TargetPlatform.class);
            for (PlatformAwareSdxDeleteService<?> platformAwareSdxDeleteService : platformDependentSdxDeleteServices.get()) {
                bean.put(platformAwareSdxDeleteService.targetPlatform(), platformAwareSdxDeleteService);
            }
            return Maps.immutableEnumMap(bean);
        } else {
            return Map.of();
        }
    }

    @Bean
    public Map<TargetPlatform, PlatformAwareSdxStartStopService> platformDependentSdxStartStopServicesMap() {
        if (platformDependentSdxStartStopServices.isPresent()) {
            Map<TargetPlatform, PlatformAwareSdxStartStopService> bean = new EnumMap<>(TargetPlatform.class);
            for (PlatformAwareSdxStartStopService platformAwareSdxStartStopService : platformDependentSdxStartStopServices.get()) {
                bean.put(platformAwareSdxStartStopService.targetPlatform(), platformAwareSdxStartStopService);
            }
            return Maps.immutableEnumMap(bean);
        } else {
            return Map.of();
        }
    }

    @Bean
    public Map<TargetPlatform, PlatformAwareSdxDescribeService> platformDependentSdxDescribeServicesMap() {
        if (platformDependentSdxDescribeServices.isPresent()) {
            Map<TargetPlatform, PlatformAwareSdxDescribeService> bean = new EnumMap<>(TargetPlatform.class);
            for (PlatformAwareSdxDescribeService platformAwareSdxDescribeService : platformDependentSdxDescribeServices.get()) {
                bean.put(platformAwareSdxDescribeService.targetPlatform(), platformAwareSdxDescribeService);
            }
            return Maps.immutableEnumMap(bean);
        } else {
            return Map.of();
        }
    }

    @Bean
    public Map<TargetPlatform, PlatformAwareSdxDhTearDownService> platformDependentSdxDhTearDownServices() {
        if (platformDependentSdxDhTearDownServices.isPresent()) {
            Map<TargetPlatform, PlatformAwareSdxDhTearDownService> bean = new EnumMap<>(TargetPlatform.class);
            for (PlatformAwareSdxDhTearDownService platformAwareSdxDhTearDownService : platformDependentSdxDhTearDownServices.get()) {
                bean.put(platformAwareSdxDhTearDownService.targetPlatform(), platformAwareSdxDhTearDownService);
            }
            return Maps.immutableEnumMap(bean);
        } else {
            return Map.of();
        }
    }

    @Bean
    @ConditionalOnMissingBean(RemoteEnvironmentApiClientParams.class)
    public RemoteEnvironmentApiClientParams remoteEnvironmentApiClientParams() {
        return new RemoteEnvironmentApiClientParams(restDebug, certificateValidation, ignorePreValidation, remoteEnvironmentServerUrl());
    }

    @Bean
    @ConditionalOnMissingBean(EnvironmentApiClientParams.class)
    public EnvironmentApiClientParams environmentApiClientParams() {
        return new EnvironmentApiClientParams(restDebug, certificateValidation, ignorePreValidation, environmentServerUrl());
    }

    private String remoteEnvironmentServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(remoteEnvironmentServiceUrl + remoteEnvironmentContextPath, "http",
                remoteEnvironmentServiceId);
    }

    private String environmentServerUrl() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveUrl(environmentServiceUrl + environmentContextPath, "http",
                environmentServiceId);
    }
}
