package com.sequenceiq.common.model;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Architecture {
    UNKNOWN,
    X86_64,
    ARM64;

    public static final String ALL_ARCHITECTURE = "ALL";

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

    public static Architecture fromString(String architecture, boolean fallback) {
        if (StringUtils.isEmpty(architecture)) {
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

    /**
     * Returns the architecture name as used in Linux RPM package paths (e.g. in Cloudera Manager repo URLs).
     * ARM64 is known as "aarch64" in the Linux kernel and RPM ecosystem, while getName() returns "arm64".
     * X86_64 is the same in both conventions.
     */
    public String getRpmName() {
        return switch (this) {
            case ARM64 -> "aarch64";
            case X86_64 -> "x86_64";
            default -> getName();
        };
    }

    @Override
    public String toString() {
        return getName();
    }
}
