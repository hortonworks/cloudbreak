package com.sequenceiq.cloudbreak.rotation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.sequenceiq.cloudbreak.rotation.serialization.SecretRotationEnumSerializationUtil;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretListField;

public class EnforceRotationFrameworkRulesUtil {

    private EnforceRotationFrameworkRulesUtil() {

    }

    public static void enforceThereAreNoDuplicatesBetweenSecretTypeEnums() {
        Set<String> duplicatedSecretTypeValues = getDuplicatedSecretTypeValues(getSecretTypeStream());
        assertTrue(duplicatedSecretTypeValues.isEmpty(), "The following secret types are appearing in multiple secret type enums, " +
                "which can break serialization easily: " + Joiner.on(",").join(duplicatedSecretTypeValues));
    }

    public static void enforceMessagesForTypesAndSteps(MessageSource messageSource) {
        Stream.concat(getSecretTypeStream(), getRotationStepStream())
                .forEach(rotationEnum -> {
                    String code = rotationEnum.getClazz().getSimpleName() + "." + SecretListField.DESCRIPTION +  "." + rotationEnum.value();
                    assertDoesNotThrow(() -> messageSource.getMessage(code, null, Locale.getDefault()),
                            String.format("Rotation enum %s does not have corresponding entry in messages property file.", rotationEnum));
                });
        getSecretTypeStream()
                .map(rotationEnum -> (SecretType) rotationEnum)
                .filter(Predicate.not(SecretType::internal))
                .forEach(rotationEnum -> {
                    String code = rotationEnum.getClazz().getSimpleName() + "." + SecretListField.DISPLAY_NAME +  "." + rotationEnum.value();
                    assertDoesNotThrow(() -> messageSource.getMessage(code, null, Locale.getDefault()),
                            String.format("Rotation enum %s does not have corresponding entry in messages property file.", rotationEnum));
                });
    }

    private static Set<String> getDuplicatedSecretTypeValues(Stream<? extends SerializableRotationEnum> enumStream) {
        Map<String, Long> secretTypeCounts = enumStream
                .map(SecretRotationEnumSerializationUtil::serialize)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return secretTypeCounts
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
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
