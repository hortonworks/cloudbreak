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
            Map<SecretType, RotationContextProvider> beans = Maps.newHashMap();
            for (RotationContextProvider rotationContextProvider : rotationContextProviders.get()) {
                beans.put(rotationContextProvider.getSecret(), rotationContextProvider);
            }
            return beans;
        } else {
            return Map.of();
        }
    }

    @Bean
    public Map<SecretRotationStep, RotationExecutor> rotationExecutorMap() {
        if (rotationExecutors.isPresent()) {
            Map<SecretRotationStep, RotationExecutor> bean = new EnumMap<>(SecretRotationStep.class);
            for (RotationExecutor rotationExecutor : rotationExecutors.get()) {
                bean.put(rotationExecutor.getType(), rotationExecutor);
            }
            return Maps.immutableEnumMap(bean);
        } else {
            return Map.of();
        }
    }
}
