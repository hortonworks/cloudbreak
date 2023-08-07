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

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretTypeConverter.class);

    private SecretTypeConverter() {

    }

    public static <T extends Enum<T> & SecretType> List<SecretType> mapSecretTypes(List<String> secrets) {
        return mapSecretTypes(secrets, AVAILABLE_SECRET_TYPES);
    }

    public static <T extends Enum<T> & SecretType> List<SecretType> mapSecretTypes(List<String> secrets,
            Set<Class<? extends SecretType>> allowedSecretTypes) {
        try {
            return secrets.stream()
                    .map(secretString -> getSecretType(secretString, allowedSecretTypes).orElseThrow())
                    .toList();
        } catch (Exception e) {
            String message = String.format("Invalid secret type, cannot map secrets %s.", secrets);
            LOGGER.warn(message);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public static <T extends Enum<T> & SecretType> SecretType mapSecretType(String secret,
            Set<Class<? extends SecretType>> allowedSecretTypes) {
        try {
            return getSecretType(secret, allowedSecretTypes).orElseThrow();
        } catch (Exception e) {
            String message = String.format("Invalid secret type, cannot map secret %s.", secret);
            LOGGER.warn(message);
            throw new CloudbreakServiceException(message, e);
        }
    }

    private static Optional<SecretType> getSecretType(String secret, Set<Class<? extends SecretType>> allowedSecretTypes) {
        return allowedSecretTypes
                .stream()
                .filter(supportedSecretType -> secretStringMatches(secret, supportedSecretType))
                .map(supportedSecretType -> getSecretTypeByClass(secret, supportedSecretType))
                .findFirst();
    }

    private static SecretType getSecretTypeByClass(String secret, Class<? extends SecretType> supportedSecretType) {
        return Arrays.stream(supportedSecretType.getEnumConstants())
                .filter(enumConstant -> StringUtils.equals(enumConstant.value(), secret))
                .findFirst()
                .orElseThrow();
    }

    private static boolean secretStringMatches(String secret, Class<? extends SecretType> supportedSecretType) {
        SecretType[] enumConstants = supportedSecretType.getEnumConstants();
        return Arrays.stream(enumConstants).anyMatch(enumConstant -> StringUtils.equals(enumConstant.value(), secret));
    }
}
