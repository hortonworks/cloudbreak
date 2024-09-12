package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Strings;

public enum Architecture {
    UNKNOWN,
    X86_64,
    ARM64;

    public static Architecture fromStringWithFallback(String architecture) {
        return fromString(architecture, true);
    }

    public static Architecture fromStringWithValidation(String architecture) {
        Architecture result = fromString(architecture, false);
        if (result == UNKNOWN) {
            throw new IllegalArgumentException(String.format("Architecture '%s' is not supported", architecture));
        }
        return result;
    }

    private static Architecture fromString(String architecture, boolean fallback) {
        if (Strings.isNullOrEmpty(architecture)) {
            return fallback ? X86_64 : null;
        }
        if (Arrays.stream(values()).noneMatch(arch -> arch.name().equalsIgnoreCase(architecture))) {
            return UNKNOWN;
        }
        return valueOf(architecture.toUpperCase());
    }

    @JsonValue
    public String getName() {
        return name().toLowerCase();
    }
}
