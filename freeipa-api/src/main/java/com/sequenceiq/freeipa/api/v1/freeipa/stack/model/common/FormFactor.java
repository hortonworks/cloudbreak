package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.stream.Stream;

public enum FormFactor {

    HA(3),
    TWO_NODE_BASED(2),
    NON_HA(1);

    private static final Map<Integer, FormFactor> FORM_FACTOR_MAP_BY_INSTANCE_COUNT;

    private final int instanceCount;

    FormFactor(int instanceCount) {
        this.instanceCount = instanceCount;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    static {
        FORM_FACTOR_MAP_BY_INSTANCE_COUNT = Stream.of(FormFactor.values())
                .collect(toMap(FormFactor::getInstanceCount, identity()));
    }

    public static FormFactor getByInstanceCount(Integer instanceCount) {
        return FORM_FACTOR_MAP_BY_INSTANCE_COUNT.get(instanceCount);
    }
}
