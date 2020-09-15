package com.sequenceiq.cloudbreak.cloud.gcp;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.gcp.service.checker.AbstractGcpComputeBaseResourceChecker;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public abstract class AbstractGcpResourceBuilder extends AbstractGcpComputeBaseResourceChecker implements CloudPlatformAware {

    @Override
    public Platform platform() {
        return GcpConstants.GCP_PLATFORM;
    }

    @Override
    public Variant variant() {
        return GcpConstants.GCP_VARIANT;
    }
}
