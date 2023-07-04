package com.sequenceiq.cloudbreak.rotation.common;

public class RotationContext {

    private final String resourceCrn;

    public RotationContext(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }
}
