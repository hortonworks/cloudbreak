package com.sequenceiq.cloudbreak.rotation.secret;

public class RotationContext {

    private final String resourceCrn;

    public RotationContext(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }
}
