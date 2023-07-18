package com.sequenceiq.cloudbreak.rotation;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class SecretTypeConverter {

    public static final Set<Class<? extends SecretType>> AVAILABLE_SECRET_TYPES =
            new Reflections("com.sequenceiq", Scanners.SubTypes)
                    .getSubTypesOf(SecretType.class)
                    .stream()
                    .filter(Class::isEnum)
                    .collect(Collectors.toSet());

    public static final Set<Class<? extends MultiSecretType>> AVAILABLE_MULTI_SECRET_TYPES =
            new Reflections("com.sequenceiq", Scanners.SubTypes)
                    .getSubTypesOf(MultiSecretType.class)
                    .stream()
                    .filter(Class::isEnum)
                    .collect(Collectors.toSet());

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretTypeConverter.class);

    private SecretTypeConverter() {

    }

    public static <T extends Enum<T> & SecretType> List<SecretType> mapSecretTypes(List<String> secrets) {
        try {
            return secrets.stream()
                    .map(secretString -> getSecretType(secretString).orElseThrow())
                    .toList();
        } catch (Exception e) {
            String message = String.format("Invalid secret type, cannot map secrets %s.", secrets);
            LOGGER.warn(message);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public static <T extends Enum<T> & SecretType> SecretType mapSecretType(String secret) {
        try {
            return getSecretType(secret).orElseThrow();
        } catch (Exception e) {
            String message = String.format("Invalid secret type, cannot map secret %s.", secret);
            LOGGER.warn(message);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public static <T extends Enum<T> & MultiSecretType> MultiSecretType mapMultiSecretType(String secret) {
        try {
            return getMultiSecretType(secret).orElseThrow();
        } catch (Exception e) {
            String message = String.format("Invalid secret type, cannot map secret %s.", secret);
            LOGGER.warn(message);
            throw new CloudbreakServiceException(message, e);
        }
    }

    private static Optional<SecretType> getSecretType(String secret) {
        return AVAILABLE_SECRET_TYPES
                .stream()
                .filter(supportedSecretType -> secretStringMatches(secret, supportedSecretType))
                .map(supportedSecretType -> getSecretTypeByClass(secret, supportedSecretType))
                .findFirst();
    }

    private static Optional<MultiSecretType> getMultiSecretType(String secret) {
        return AVAILABLE_MULTI_SECRET_TYPES
                .stream()
                .filter(supportedSecretType -> multiSecretStringMatches(secret, supportedSecretType))
                .map(supportedSecretType -> getMultiSecretTypeByClass(secret, supportedSecretType))
                .findFirst();
    }

    private static SecretType getSecretTypeByClass(String secret, Class<? extends SecretType> supportedSecretType) {
        return Arrays.stream(supportedSecretType.getEnumConstants())
                .filter(enumConstant -> StringUtils.equals(enumConstant.value(), secret))
                .findFirst()
                .orElseThrow();
    }

    private static MultiSecretType getMultiSecretTypeByClass(String secret, Class<? extends MultiSecretType> supportedSecretType) {
        return Arrays.stream(supportedSecretType.getEnumConstants())
                .filter(enumConstant -> StringUtils.equals(enumConstant.value(), secret))
                .findFirst()
                .orElseThrow();
    }

    private static boolean secretStringMatches(String secret, Class<? extends SecretType> supportedSecretType) {
        SecretType[] enumConstants = supportedSecretType.getEnumConstants();
        return Arrays.stream(enumConstants).anyMatch(enumConstant -> StringUtils.equals(enumConstant.value(), secret));
    }

    private static boolean multiSecretStringMatches(String secret, Class<? extends MultiSecretType> supportedSecretType) {
        MultiSecretType[] enumConstants = supportedSecretType.getEnumConstants();
        return Arrays.stream(enumConstants).anyMatch(enumConstant -> StringUtils.equals(enumConstant.value(), secret));
    }
}
