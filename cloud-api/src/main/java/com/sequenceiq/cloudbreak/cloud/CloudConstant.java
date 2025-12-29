package com.sequenceiq.cloudbreak.cloud;

import java.util.Arrays;

public interface CloudConstant extends CloudPlatformAware {

    default boolean hasVariants(String variant) {
        return variants() != null && Arrays.stream(variants())
                .filter(e -> e.equalsIgnoreCase(variant))
                .findFirst()
                .isPresent();
    }

    String[] variants();
}
