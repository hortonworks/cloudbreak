package com.sequenceiq.common.model;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Architecture {
    UNKOWN,
    X86_64,
    ARM64;

    public static Architecture fromStringWithFallback(String architecture) {
        return fromString(architecture, true);
    }

    public static Architecture fromStringWithValidation(String architecture) {
        Architecture result = fromString(architecture, false);
        if (result == UNKOWN) {
            throw new IllegalArgumentException(String.format("Architecture '%s' is not supported", architecture));
        }
        return result;
    }

    private static Architecture fromString(String architecture, boolean fallback) {
        if (StringUtils.isEmpty(architecture)) {
            return fallback ? X86_64 : null;
        }
        if (Arrays.stream(values()).noneMatch(arch -> arch.name().equalsIgnoreCase(architecture))) {
            return UNKOWN;
        }
        return valueOf(architecture.toUpperCase());
    }

    @JsonValue
    public String getName() {
        return name().toLowerCase();
    }

    @Override
    public String toString() {
        return getName();
    }
}
