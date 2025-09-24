package com.sequenceiq.cloudbreak.cloud.model.component;

import static com.sequenceiq.cloudbreak.common.type.ComponentType.cdhProductDetails;

import com.sequenceiq.cloudbreak.common.type.ComponentType;

//TODO: this shouldn't be in cloud-common, but ComponentType is in cloud-common too..
public enum StackType {
    HDP(ComponentType.HDP_REPO_DETAILS),
    // Currently only HDP_REPO_DETAILS is handled
    HDF(ComponentType.HDP_REPO_DETAILS),

    CDH(cdhProductDetails());

    private final ComponentType componentType;

    StackType(ComponentType componentType) {
        this.componentType = componentType;
    }

    public ComponentType getComponentType() {
        return componentType;
    }
}
