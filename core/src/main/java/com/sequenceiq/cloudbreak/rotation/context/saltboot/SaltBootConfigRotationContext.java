package com.sequenceiq.cloudbreak.rotation.context.saltboot;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public abstract class SaltBootConfigRotationContext extends RotationContext {

    public SaltBootConfigRotationContext(String resourceCrn) {
        super(resourceCrn);
    }

    public abstract SaltBootUpdateConfiguration getServiceUpdateConfiguration();
}
