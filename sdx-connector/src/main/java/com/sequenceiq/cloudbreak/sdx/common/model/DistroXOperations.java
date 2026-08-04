package com.sequenceiq.cloudbreak.sdx.common.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum DistroXOperations {
    CREATE,
    START;

    private static final Set<DistroXOperations> DISTROX_OPERATIONS = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableSet());

    public static Set<DistroXOperations> getDistroxOperations() {
        return DISTROX_OPERATIONS;
    }
}