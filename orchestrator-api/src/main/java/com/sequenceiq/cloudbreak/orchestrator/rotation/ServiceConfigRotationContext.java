package com.sequenceiq.cloudbreak.orchestrator.rotation;

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;

public abstract class ServiceConfigRotationContext extends RotationContext {

    public ServiceConfigRotationContext(String resourceCrn) {
        super(resourceCrn);
    }

    public abstract ServiceUpdateConfiguration getServiceUpdateConfiguration();
}
