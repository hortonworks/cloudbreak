package com.sequenceiq.cloudbreak.saas.config;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.saas.sdx.SdxService;
import com.sequenceiq.cloudbreak.saas.sdx.TargetPlatform;

@Configuration
public class PlatformConfig {

    @Inject
    private Optional<List<SdxService>> platformDependentServices;

    @Bean
    public Map<TargetPlatform, SdxService> platformDependentServicesMap() {
        if (platformDependentServices.isPresent()) {
            Map<TargetPlatform, SdxService> bean = new EnumMap<>(TargetPlatform.class);
            for (SdxService platformDependentService : platformDependentServices.get()) {
                bean.put(platformDependentService.targetPlatform(), platformDependentService);
            }
            return Maps.immutableEnumMap(bean);
        } else {
            return Map.of();
        }
    }
}
