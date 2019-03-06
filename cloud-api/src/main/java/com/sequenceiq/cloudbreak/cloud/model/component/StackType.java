package com.sequenceiq.cloudbreak.cloud.model.component;

import com.sequenceiq.cloudbreak.common.type.ComponentType;

public enum StackType {
    HDP(ComponentType.HDP_REPO_DETAILS),
    // Currently only HDP_REPO_DETAILS is handled
    HDF(ComponentType.HDP_REPO_DETAILS),

    CDH(ComponentType.CDH_PRODUCT_DETAILS);

    private final ComponentType componentType;

    StackType(ComponentType componentType) {
        this.componentType = componentType;
    }

    public ComponentType getComponentType() {
        return componentType;
    }
}
