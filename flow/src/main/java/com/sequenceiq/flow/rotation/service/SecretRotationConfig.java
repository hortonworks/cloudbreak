package com.sequenceiq.flow.rotation.service;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.application.ApplicationSecretRotationInformation;

@Configuration
public class SecretRotationConfig {

    @Inject
    private Optional<ApplicationSecretRotationInformation> applicationSecretRotationInformation;

    @Inject
    private Optional<List<RotationContextProvider>> rotationContextProviders;

    @Inject
    private Optional<List<RotationExecutor<? extends RotationContext>>> rotationExecutors;

    @Bean
    public Map<SecretType, RotationContextProvider> rotationContextProviderMap() {
        if (applicationSecretRotationInformation.isPresent() && rotationContextProviders.isPresent()) {
            Map<SecretType, RotationContextProvider> beans = Maps.newHashMap();
            for (RotationContextProvider rotationContextProvider : rotationContextProviders.get()) {
                Class<? extends SecretType> supportedSecretType = applicationSecretRotationInformation.get().supportedSecretType();
                if (rotationContextProvider.getSecret().getClass().isAssignableFrom(supportedSecretType)) {
                    beans.put(rotationContextProvider.getSecret(), rotationContextProvider);
                }
            }
            return beans;
        } else {
            return Map.of();
        }
    }

    @Bean
    public Map<SecretRotationStep, RotationExecutor<? extends RotationContext>> rotationExecutorMap() {
        if (applicationSecretRotationInformation.isPresent() && rotationExecutors.isPresent()) {
            Class<? extends SecretType> supportedSecretType = applicationSecretRotationInformation.get().supportedSecretType();
            Set<SecretRotationStep> supportedSteps = Arrays.stream(supportedSecretType.getEnumConstants())
                    .flatMap(secretType -> secretType.getSteps().stream())
                    .collect(Collectors.toSet());
            Map<SecretRotationStep, RotationExecutor<? extends RotationContext>> bean = new EnumMap<>(SecretRotationStep.class);
            for (RotationExecutor<? extends RotationContext> rotationExecutor : rotationExecutors.get()) {
                if (supportedSteps.contains(rotationExecutor.getType())) {
                    bean.put(rotationExecutor.getType(), rotationExecutor);
                }
            }
            return Maps.immutableEnumMap(bean);
        } else {
            return Map.of();
        }
    }
}
