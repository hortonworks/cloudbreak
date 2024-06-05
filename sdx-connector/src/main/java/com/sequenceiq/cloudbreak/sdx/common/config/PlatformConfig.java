package com.sequenceiq.cloudbreak.sdx.common.config;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDeleteService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxStatusService;

@Configuration
public class PlatformConfig {

    @Inject
    private Optional<List<PlatformAwareSdxStatusService<?>>> platformDependentSdxStatusServices;

    @Inject
    private Optional<List<PlatformAwareSdxDeleteService<?>>> platformDependentSdxDeleteServices;

    @Inject
    private Optional<List<PlatformAwareSdxDescribeService>> platformDependentSdxDescribeServices;

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
}
