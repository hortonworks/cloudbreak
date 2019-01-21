package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum InstanceGroupType {
    GATEWAY, CORE;

    public static boolean isGateway(InstanceGroupType type) {
        return GATEWAY.equals(type);
    }

    public static String all() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(","));
    }
}