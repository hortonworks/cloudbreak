package com.sequenceiq.freeipa.service.rotation.saltboot.context;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.freeipa.service.rotation.saltboot.contextprovider.SaltBootUpdateConfiguration;

public abstract class SaltBootConfigRotationContext extends RotationContext {

    public SaltBootConfigRotationContext(String resourceCrn) {
        super(resourceCrn);
    }

    public abstract SaltBootUpdateConfiguration getServiceUpdateConfiguration();
}
