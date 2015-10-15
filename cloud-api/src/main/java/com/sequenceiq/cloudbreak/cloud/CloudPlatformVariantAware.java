package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.model.Variant;

public interface CloudPlatformVariantAware extends CloudPlatformAware {
    Variant variant();
}
