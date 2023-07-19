package com.sequenceiq.cloudbreak.rotation.common;

import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationMetadata;

public interface MultiClusterRotationContextProvider extends RotationContextProvider {

    MultiClusterRotationMetadata getMultiClusterRotationMetadata(String resourceCrn);
}
