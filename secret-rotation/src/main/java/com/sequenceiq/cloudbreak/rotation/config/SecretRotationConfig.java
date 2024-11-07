package com.sequenceiq.cloudbreak.rotation.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeConverter;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;

@Configuration
public class SecretRotationConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationConfig.class);

    private static final Set<Class<? extends SecretType>> AVAILABLE_SECRET_TYPES =
            new Reflections("com.sequenceiq", Scanners.SubTypes)
                    .getSubTypesOf(SecretType.class)
                    .stream()
                    .filter(Class::isEnum)
                    .collect(Collectors.toSet());

    @Value("${aws.use.fips.endpoint:false}")
    private boolean fipsEnabled;

    @Value("${secretrotation.commercialEnabledSecretTypes:}")
    private List<String> commercialEnabledSecretTypes;

    @Value("${secretrotation.secretTypeEnumClass:}")
    private String secretTypeEnumClass;

    @Inject
    private Optional<List<RotationContextProvider>> rotationContextProviders;

    @Inject
    private Optional<List<AbstractRotationExecutor<? extends RotationContext>>> rotationExecutors;

    @Bean
    public Map<SecretType, RotationContextProvider> rotationContextProviderMap() {
        if (rotationContextProviders.isPresent()) {
            Map<SecretType, RotationContextProvider> beans = Maps.newHashMap();
            for (RotationContextProvider rotationContextProvider : rotationContextProviders.get()) {
                if (enabledSecretTypes().stream()
                        .anyMatch(secretType -> rotationContextProvider.getSecret().getClass().isAssignableFrom(secretType.getClass()))) {
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
            Set<SecretRotationStep> supportedSteps = enabledSecretTypes().stream()
                    .map(SecretType::getSteps)
                    .flatMap(List::stream)
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
        Class<? extends SecretType> appBasedSecretTypeClass = AVAILABLE_SECRET_TYPES.stream()
                .filter(typeClass -> StringUtils.equals(typeClass.getSimpleName(), secretTypeEnumClass))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No secret type present for this application!"));
        if (!fipsEnabled && CollectionUtils.isNotEmpty(commercialEnabledSecretTypes)) {
            LOGGER.debug("Secret types filtering is enabled, secret types: {}", commercialEnabledSecretTypes);
            List<SecretType> enabledSecretTypes = SecretTypeConverter.mapSecretTypesSkipUnknown(commercialEnabledSecretTypes, Set.of(appBasedSecretTypeClass));
            List<SecretType> internalSecretTypes = enabledSecretTypes.stream().filter(SecretType::internal).toList();
            if (!internalSecretTypes.isEmpty()) {
                throw new IllegalStateException(String.format("Do not configure internal secret types in commercialEnabledSecretTypes filter: %s",
                        internalSecretTypes));
            }
            return enabledSecretTypes;
        } else {
            LOGGER.debug("Secret types filtering is disabled");
            return List.of(appBasedSecretTypeClass.getEnumConstants());
        }
    }
}
