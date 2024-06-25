package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Arrays;

import com.google.common.base.Strings;

public enum Architecture {
    UNKOWN,
    X86_64,
    ARM64;

    public static Architecture fromString(String architecture) {
        if (Strings.isNullOrEmpty(architecture)) {
            return X86_64;
        }
        if (Arrays.stream(values()).noneMatch(arch -> arch.name().equalsIgnoreCase(architecture))) {
            return UNKOWN;
        }
        return valueOf(architecture.toUpperCase());
    }
}
