package com.sequenceiq.cloudbreak.common.type;

import java.util.Arrays;

import jakarta.persistence.AttributeConverter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

public enum KdcType {
    UNKNOWN,
    ACTIVE_DIRECTORY,
    MIT;

    private static final Logger LOGGER = LoggerFactory.getLogger(KdcType.class);

    public static KdcType fromStringWithFallback(String kdcType) {
        if (StringUtils.isEmpty(kdcType)) {
            return ACTIVE_DIRECTORY;
        }
        if (Arrays.stream(values()).noneMatch(arch -> arch.name().equalsIgnoreCase(kdcType))) {
            LOGGER.info("KDC type {} is unknown", kdcType);
            return UNKNOWN;
        }
        return valueOf(kdcType.toUpperCase());
    }

    public static KdcType fromStringWithValidation(String kdcType) {
        KdcType result = fromStringWithFallback(kdcType);
        if (result == UNKNOWN) {
            throw new IllegalArgumentException(String.format("KDC type '%s' is not supported. Supported values: %s", kdcType, Joiner.on(", ").join(values())));
        }
        return result;
    }

    public static class Converter implements AttributeConverter<KdcType, String> {

        @Override
        public String convertToDatabaseColumn(KdcType kdcType) {
            return kdcType.name();
        }

        @Override
        public KdcType convertToEntityAttribute(String s) {
            return fromStringWithFallback(s);
        }
    }
}
