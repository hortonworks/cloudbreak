package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.stream.Stream;

public enum AvailabilityType {

    HA(3),
    TWO_NODE_BASED(2),
    NON_HA(1);

    private static final Map<Integer, AvailabilityType> AVAILABILITY_TYPE_MAP_BY_INSTANCE_COUNT;

    private final int instanceCount;

    AvailabilityType(int instanceCount) {
        this.instanceCount = instanceCount;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    static {
        AVAILABILITY_TYPE_MAP_BY_INSTANCE_COUNT = Stream.of(AvailabilityType.values())
                .collect(toMap(AvailabilityType::getInstanceCount, identity()));
    }

    public static AvailabilityType getByInstanceCount(Integer instanceCount) {
        return AVAILABILITY_TYPE_MAP_BY_INSTANCE_COUNT.get(instanceCount);
    }
}
