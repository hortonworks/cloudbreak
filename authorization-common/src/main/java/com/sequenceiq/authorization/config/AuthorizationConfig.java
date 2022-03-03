package com.sequenceiq.authorization.config;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Maps;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.AuthorizationEnvironmentCrnListProvider;
import com.sequenceiq.authorization.service.AuthorizationEnvironmentCrnProvider;
import com.sequenceiq.authorization.service.AuthorizationResourceCrnListProvider;
import com.sequenceiq.authorization.service.AuthorizationResourceCrnProvider;
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
    public Map<AuthorizationResourceType, AuthorizationEnvironmentCrnListProvider> environmentCrnListProviderMap() {
        return resourceProviderMap(AuthorizationEnvironmentCrnListProvider.class);
    }

    @Bean
    public Map<AuthorizationResourceType, AuthorizationEnvironmentCrnProvider> environmentCrnProviderMap() {
        return resourceProviderMap(AuthorizationEnvironmentCrnProvider.class);
    }

    @Bean
    public Map<AuthorizationResourceType, AuthorizationResourceCrnProvider> resourceCrnProviderMap() {
        return resourceProviderMap(AuthorizationResourceCrnProvider.class);
    }

    @Bean
    public Map<AuthorizationResourceType, AuthorizationResourceCrnListProvider> resourceCrnListProviderMap() {
        return resourceProviderMap(AuthorizationResourceCrnListProvider.class);
    }

    private <T extends ResourcePropertyProvider> Map<AuthorizationResourceType, T> resourceProviderMap(Class<T> clazz) {
        if (resourceBasedCrnProviders.isPresent()) {
            Map<AuthorizationResourceType, T> bean = new EnumMap<>(AuthorizationResourceType.class);
            for (ResourcePropertyProvider resourcePropertyProvider : resourceBasedCrnProviders.get()) {
                AuthorizationResourceType supportedAuthorizationResourceType = resourcePropertyProvider.getSupportedAuthorizationResourceType();
                if (Objects.nonNull(supportedAuthorizationResourceType) && clazz.isAssignableFrom(resourcePropertyProvider.getClass())) {
                    bean.put(supportedAuthorizationResourceType, (T) resourcePropertyProvider);
                }
            }
            return Maps.immutableEnumMap(bean);
        } else {
            return Map.of();
        }
    }
}
