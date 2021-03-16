package com.sequenceiq.authorization.config;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Maps;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.ResourcePropertyProvider;
import com.sequenceiq.authorization.service.defaults.DefaultResourceChecker;

@Configuration
public class AuthorizationConfig {

    @Inject
    private Optional<List<DefaultResourceChecker>> defaultResourceCheckers;

    @Inject
    private Optional<List<ResourcePropertyProvider>> resourceBasedCrnProviders;

    @Bean
    public Map<AuthorizationResourceType, DefaultResourceChecker> defaultResourceCheckerMap() {
        if (defaultResourceCheckers.isPresent()) {
            Map<AuthorizationResourceType, DefaultResourceChecker> bean = new EnumMap<>(AuthorizationResourceType.class);
            for (DefaultResourceChecker defaultResourceChecker : defaultResourceCheckers.get()) {
                bean.put(defaultResourceChecker.getResourceType(), defaultResourceChecker);
            }
            return Maps.immutableEnumMap(bean);
        } else {
            return Map.of();
        }
    }

    @Bean
    public Map<AuthorizationResourceType, ResourcePropertyProvider> resourceBasedCrnProviderMap() {
        if (resourceBasedCrnProviders.isPresent()) {
            Map<AuthorizationResourceType, ResourcePropertyProvider> bean = new EnumMap<>(AuthorizationResourceType.class);
            for (ResourcePropertyProvider resourceBasedCrnProvider : resourceBasedCrnProviders.get()) {
                if (resourceBasedCrnProvider.getSupportedAuthorizationResourceType().isPresent()) {
                    bean.put(resourceBasedCrnProvider.getSupportedAuthorizationResourceType().get(), resourceBasedCrnProvider);
                }
            }
            return Maps.immutableEnumMap(bean);
        } else {
            return Map.of();
        }
    }
}
