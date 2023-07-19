package com.sequenceiq.cloudbreak.rotation.service.multicluster;

import com.sequenceiq.cloudbreak.rotation.MultiSecretType;

public interface InterServiceMultiClusterRotationService {

    boolean checkOngoingChildrenMultiSecretRotations(String parentResourceCrn, MultiSecretType multiSecretType);

    void markChildren(String parentResourceCrn, MultiSecretType multiSecretType);
}
