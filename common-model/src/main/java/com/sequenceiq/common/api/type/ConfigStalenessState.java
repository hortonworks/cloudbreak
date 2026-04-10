package com.sequenceiq.common.api.type;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

public enum ConfigStalenessState {
    UP_TO_DATE,
    STALE,
    UNKNOWN;

    public static ConfigStalenessState fromString(String configStalenessState) {
        if (StringUtils.isEmpty(configStalenessState)) {
            return UP_TO_DATE;
        }
        if (Arrays.stream(values()).noneMatch(arch -> arch.name().equalsIgnoreCase(configStalenessState))) {
            return UNKNOWN;
        }
        return valueOf(configStalenessState.toUpperCase());
    }
}
