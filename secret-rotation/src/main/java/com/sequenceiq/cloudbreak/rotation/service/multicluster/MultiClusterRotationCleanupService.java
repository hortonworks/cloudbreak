package com.sequenceiq.cloudbreak.rotation.service.multicluster;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType;

@Service
public class MultiClusterRotationCleanupService {

    @Inject
    private MultiClusterRotationTrackingService multiClusterRotationTrackingService;

    public void cleanupAfterRollback(String resourceCrn, MultiClusterRotationMetadata metadata) {
        multiClusterRotationTrackingService.getResourceForSecretType(resourceCrn, metadata.secretType())
                .ifPresent(multiClusterRotationResource -> {
                    if (multiClusterRotationResource.getType().equals(MultiClusterRotationResourceType.PARENT_INITIAL)) {
                        multiClusterRotationTrackingService.clearAllForSecretType(metadata);
                    }
                });
    }

    public void cleanupAfterFinalize(String resourceCrn, MultiClusterRotationMetadata metadata) {
        multiClusterRotationTrackingService.getResourceForSecretType(resourceCrn, metadata.secretType())
                .ifPresent(multiClusterRotationResource -> {
                    if (multiClusterRotationResource.getType().equals(MultiClusterRotationResourceType.PARENT_INITIAL)) {
                        multiClusterRotationTrackingService.switchParentToFinalPhase(multiClusterRotationResource);
                    } else {
                        multiClusterRotationTrackingService.clearResourceForSecretType(resourceCrn, metadata.secretType());
                    }
                });
    }
}
