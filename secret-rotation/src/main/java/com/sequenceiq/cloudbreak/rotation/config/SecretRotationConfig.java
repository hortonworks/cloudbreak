package com.sequenceiq.cloudbreak.rotation.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeConverter;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;

@Configuration
public class SecretRotationConfig {

    @Value("${aws.use.fips.endpoint:false}")
    private boolean fipsEnabled;

    @Value("${secretrotation.commercialEnabledSecretTypes:}")
    private List<String> commercialEnabledSecretTypes;

    @Inject
    private Optional<List<RotationContextProvider>> rotationContextProviders;

    @Inject
    private Optional<List<AbstractRotationExecutor<? extends RotationContext>>> rotationExecutors;

    @Bean
    public Map<SecretType, RotationContextProvider> rotationContextProviderMap() {
        if (rotationContextProviders.isPresent()) {
            Map<SecretType, RotationContextProvider> beans = Maps.newHashMap();
            for (RotationContextProvider rotationContextProvider : rotationContextProviders.get()) {
                Set<Class<? extends SecretType>> supportedSecretTypes = SecretTypeConverter.AVAILABLE_SECRET_TYPES;
                if (supportedSecretTypes.stream().anyMatch(secretType -> rotationContextProvider.getSecret().getClass().isAssignableFrom(secretType))) {
                    beans.put(rotationContextProvider.getSecret(), rotationContextProvider);
                }
            }
            return beans;
        } else {
            return Map.of();
        }
    }

    @Bean
    public Map<SecretRotationStep, AbstractRotationExecutor<? extends RotationContext>> rotationExecutorMap() {
        if (rotationExecutors.isPresent()) {
            Set<Class<? extends SecretType>> supportedSecretTypes = SecretTypeConverter.AVAILABLE_SECRET_TYPES;
            Set<SecretRotationStep> supportedSteps = supportedSecretTypes.stream()
                    .map(secretType -> Arrays.stream(secretType.getEnumConstants())
                            .flatMap(secretTypeConstant -> secretTypeConstant.getSteps().stream())
                            .collect(Collectors.toSet()))
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
            Map<SecretRotationStep, AbstractRotationExecutor<? extends RotationContext>> beans = Maps.newHashMap();
            for (AbstractRotationExecutor<? extends RotationContext> rotationExecutor : rotationExecutors.get()) {
                if (supportedSteps.contains(rotationExecutor.getType())) {
                    beans.put(rotationExecutor.getType(), rotationExecutor);
                }
            }
            return beans;
        } else {
            return Map.of();
        }
    }

    @Bean
    public List<SecretType> enabledSecretTypes() {
        if (!fipsEnabled && CollectionUtils.isNotEmpty(commercialEnabledSecretTypes)) {
            List<SecretType> enabledSecretTypes = SecretTypeConverter.mapSecretTypes(this.commercialEnabledSecretTypes);
            List<SecretType> internalSecretTypes = enabledSecretTypes.stream().filter(SecretType::internal).toList();
            if (!internalSecretTypes.isEmpty()) {
                throw new IllegalStateException(String.format("Do not configure internal secret types in commercialEnabledSecretTypes filter: %s",
                        internalSecretTypes));
            }
            return enabledSecretTypes;
        } else {
            return List.of();
        }
    }
}
