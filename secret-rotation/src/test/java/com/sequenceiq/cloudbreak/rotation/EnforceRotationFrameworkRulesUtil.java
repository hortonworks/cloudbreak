package com.sequenceiq.cloudbreak.rotation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;

import uk.co.jemos.podam.api.PodamFactoryImpl;

public class EnforceRotationFrameworkRulesUtil {

    public static final Set<Class<? extends RotationContextProvider>> AVAILABLE_CONTEXT_PROVIDERS =
            new Reflections("com.sequenceiq", Scanners.SubTypes)
                    .getSubTypesOf(RotationContextProvider.class)
                    .stream()
                    .filter(e -> !e.isInterface())
                    .filter(e -> !Modifier.isAbstract(e.getModifiers()))
                    .collect(Collectors.toSet());

    private static final PodamFactoryImpl SAMPLE_OBJECT_FACTORY = new PodamFactoryImpl();

    private EnforceRotationFrameworkRulesUtil() {

    }

    public static void enforceMultiSecretTypes() {
        Set<SecretType> secretTypesPlacedInMultiSecretTypeButNotMarked = Stream.concat(parentSecretTypesStream(), childSecretTypesStream())
                .filter(Predicate.not(SecretType::multiSecret))
                .collect(Collectors.toSet());
        assertTrue(secretTypesPlacedInMultiSecretTypeButNotMarked.isEmpty(),
                "The following secret types are placed in multi secret type but not marked with multi secret flag: "
                        + Joiner.on(",").join(secretTypesPlacedInMultiSecretTypeButNotMarked));
    }

    public static void enforceMultiSecretTypeMethodForRelatedContextProviders() {
        Set<String> contextProvidersForMultiSecretWithoutMultiSecretMethod =
                AVAILABLE_CONTEXT_PROVIDERS.stream()
                        .map(contextProviderClass -> SAMPLE_OBJECT_FACTORY.manufacturePojo(contextProviderClass))
                        .filter(rotationContextProvider -> rotationContextProvider.getSecret().multiSecret())
                        .filter(rotationContextProvider -> rotationContextProvider.getMultiSecret().isEmpty())
                        .map(RotationContextProvider::getClass)
                        .map(Class::getSimpleName)
                        .collect(Collectors.toSet());
        assertTrue(contextProvidersForMultiSecretWithoutMultiSecretMethod.isEmpty(), "These context providers are related to secret type with " +
                "multi secret flag but getMultiSecret() is not implemented properly: "
                + Joiner.on(",").join(contextProvidersForMultiSecretWithoutMultiSecretMethod));
    }

    public static void enforceSecretTypeBelongsOnlyOneMultiSecretType() {
        Set<String> duplicatedSecretTypeValues = getDuplicatedSecretTypeValues(Stream.concat(parentSecretTypesStream(), childSecretTypesStream()));
        assertTrue(duplicatedSecretTypeValues.isEmpty(), "These secret types are referenced in multiple multi secret types, which is not supported: "
                + Joiner.on(",").join(duplicatedSecretTypeValues));

    }

    public static void enforceThereAreNoDuplicatesBetweenSecretTypeEnums() {
        Set<String> duplicatedSecretTypeValues = getDuplicatedSecretTypeValues(getSecretTypeStream());
        assertTrue(duplicatedSecretTypeValues.isEmpty(), "The following secret types are appearing in multiple secret type enums, " +
                "which can break serialization easily: " + Joiner.on(",").join(duplicatedSecretTypeValues));
    }

    public static void enforceThereAreNoDuplicatesBetweenMultiSecretTypeEnums() {
        Set<String> duplicatedSecretTypeValues = getDuplicatedSecretTypeValues(getMultiSecretTypeStream());
        assertTrue(duplicatedSecretTypeValues.isEmpty(), "The following multi secret types are appearing in multiple multi secret type enums, " +
                "which can break serialization easily: " + Joiner.on(",").join(duplicatedSecretTypeValues));
    }

    public static void enforceMessagesForTypesAndSteps(MessageSource messageSource) {
        Stream.concat(getSecretTypeStream(), getRotationStepStream())
                .forEach(rotationEnum -> {
                    String code = rotationEnum.getClazz().getSimpleName() + "." + rotationEnum.value();
                    assertDoesNotThrow(() -> messageSource.getMessage(code, null, Locale.getDefault()),
                            String.format("Rotation enum %s does not have corresponding entry in messages property file.", rotationEnum));
                });
    }

    private static Set<String> getDuplicatedSecretTypeValues(Stream<? extends SerializableRotationEnum> enumStream) {
        Map<String, Long> secretTypeCounts = enumStream
                .map(SerializableRotationEnum::value)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return secretTypeCounts
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static Stream<SecretType> childSecretTypesStream() {
        return  getMultiSecretTypeStream()
                .map(MultiSecretType::childSecretTypesByDescriptor)
                .flatMap(map -> map.values().stream());
    }

    private static Stream<SecretType> parentSecretTypesStream() {
        return getMultiSecretTypeStream()
                .map(MultiSecretType::parentSecretType);
    }

    private static Stream<MultiSecretType> getMultiSecretTypeStream() {
        return new Reflections("com.sequenceiq", Scanners.SubTypes)
                .getSubTypesOf(MultiSecretType.class)
                .stream()
                .filter(Class::isEnum)
                .map(Class::getEnumConstants)
                .flatMap(Arrays::stream);
    }

    private static Stream<SerializableRotationEnum> getSecretTypeStream() {
        return new Reflections("com.sequenceiq", Scanners.SubTypes)
                .getSubTypesOf(SecretType.class)
                .stream()
                .filter(Class::isEnum)
                .filter(clazz -> !clazz.getSimpleName().startsWith("Test"))
                .map(Class::getEnumConstants)
                .flatMap(Arrays::stream);
    }

    private static Stream<SerializableRotationEnum> getRotationStepStream() {
        return new Reflections("com.sequenceiq", Scanners.SubTypes)
                .getSubTypesOf(SecretRotationStep.class)
                .stream()
                .filter(Class::isEnum)
                .filter(clazz -> !clazz.getSimpleName().startsWith("Test"))
                .map(Class::getEnumConstants)
                .flatMap(Arrays::stream);
    }

    @Configuration
    @EnableConfigurationProperties
    public static class TestAppContext {

        @Bean(name = "enforceRotationFrameworkRulesMessageSource")
        public MessageSource messageSource() {
            ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
            messageSource.setBasename("messages/messages");
            return messageSource;
        }

    }

}
