package com.sequenceiq.flow.rotation;

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;

public class TestRotationContext extends RotationContext {

    public TestRotationContext(String resourceCrn) {
        super(resourceCrn);
    }
}