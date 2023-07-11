package com.sequenceiq.cloudbreak.rotation.service.multicluster;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.entity.MultiClusterRotationResourceType;
import com.sequenceiq.cloudbreak.rotation.entity.MultiClusterSecretRotation;
import com.sequenceiq.cloudbreak.rotation.repository.MultiClusterSecretRotationRepository;

@Service
public class MultiClusterRotationOrchestrationService {

    @Inject
    private MultiClusterSecretRotationRepository repository;

    public boolean multiClusterParentRotationOnly(String resourceCrn, SecretType secretType) {
        return repository.findByResourceCrnAndSecretTypeAndResourceType(resourceCrn, secretType, MultiClusterRotationResourceType.PARENT).isEmpty();
    }

    public boolean multiClusterParentFinalizationOnly(String resourceCrn, SecretType secretType) {
        return repository.findByResourceCrnAndSecretTypeAndResourceType(resourceCrn, secretType, MultiClusterRotationResourceType.PARENT).isPresent();
    }

    public boolean multiClusterChildRotation(String resourceCrn, SecretType secretType) {
        return repository.findByResourceCrnAndSecretTypeAndResourceType(resourceCrn, secretType, MultiClusterRotationResourceType.CHILD).isPresent();
    }

    public void markChildClustersIfNeeded(String parentResourceCrn, SecretType secretType) {
        if (secretType.multiCluster()) {
            repository.save(new MultiClusterSecretRotation());
        }
    }
}
