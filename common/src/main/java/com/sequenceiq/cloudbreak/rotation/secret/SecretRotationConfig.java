package com.sequenceiq.cloudbreak.rotation.secret;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Maps;

@Configuration
public class SecretRotationConfig {

    @Inject
    private Optional<List<RotationContextProvider>> rotationContextProviders;

    @Inject
    private Optional<List<RotationExecutor>> rotationExecutors;

    @Bean
    public Map<SecretType, RotationContextProvider> rotationContextProviderMap() {
        if (rotationContextProviders.isPresent()) {
            Map<SecretType, RotationContextProvider> bean = new EnumMap<>(SecretType.class);
            for (RotationContextProvider rotationContextProvider : rotationContextProviders.get()) {
                bean.put(rotationContextProvider.getSecret(), rotationContextProvider);
            }
            return Maps.immutableEnumMap(bean);
        } else {
            return Map.of();
        }
    }

    @Bean
    public Map<SecretLocationType, RotationExecutor> rotationExecutorMap() {
        if (rotationExecutors.isPresent()) {
            Map<SecretLocationType, RotationExecutor> bean = new EnumMap<>(SecretLocationType.class);
            for (RotationExecutor rotationExecutor : rotationExecutors.get()) {
                bean.put(rotationExecutor.getType(), rotationExecutor);
            }
            return Maps.immutableEnumMap(bean);
        } else {
            return Map.of();
        }
    }
}
