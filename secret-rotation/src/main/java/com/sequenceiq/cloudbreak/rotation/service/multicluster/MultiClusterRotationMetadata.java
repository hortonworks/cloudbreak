package com.sequenceiq.cloudbreak.rotation.service.multicluster;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;

public record MultiClusterRotationMetadata(
    String parentResourceCrn,
    Set<String> childResourceCrns,
    MultiSecretType secretType
) {

    public Set<String> allResources() {
        return Sets.union(Set.of(parentResourceCrn), childResourceCrns);
    }

}
