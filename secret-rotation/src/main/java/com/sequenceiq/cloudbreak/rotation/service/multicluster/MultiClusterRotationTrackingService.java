package com.sequenceiq.cloudbreak.rotation.service.multicluster;

import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.CHILD;
import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.PARENT_INITIAL;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResource;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType;
import com.sequenceiq.cloudbreak.rotation.repository.MultiClusterRotationResourceRepository;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@Service
public class MultiClusterRotationTrackingService {

    @Inject
    private MultiClusterRotationResourceRepository multiClusterRotationResourceRepository;

    public boolean multiRotationNeededForResource(RotationMetadata metadata, MultiClusterRotationResourceType type) {
        return multiClusterRotationResourceRepository.findByResourceCrnAndSecretTypeAndType(
                metadata.resourceCrn(), metadata.multiClusterRotationMetadata().orElseThrow().secretType(), type).isPresent();
    }

    public void clearAllForSecretType(MultiClusterRotationMetadata metadata) {
        multiClusterRotationResourceRepository.deleteAllByResourceCrnInAndSecretType(metadata.allResources(), metadata.secretType());
    }

    public Optional<MultiClusterRotationResource> getResourceForSecretType(String resourceCrn, MultiSecretType secretType) {
        return multiClusterRotationResourceRepository.findByResourceCrnAndSecretType(resourceCrn, secretType);
    }

    public void switchParentToFinalPhase(MultiClusterRotationResource rotationResource) {
        if (rotationResource.getType().equals(MultiClusterRotationResourceType.PARENT_INITIAL)) {
            rotationResource.setType(MultiClusterRotationResourceType.PARENT_FINAL);
            multiClusterRotationResourceRepository.save(rotationResource);
        }
    }

    public void clearResourceForSecretType(String resourceCrn, MultiSecretType secretType) {
        multiClusterRotationResourceRepository.deleteByResourceCrnAndSecretType(resourceCrn, secretType);
    }

    public Set<MultiClusterRotationResource> getAll(Set<String> resources, MultiSecretType secretType) {
        return multiClusterRotationResourceRepository.findAllByResourceCrnInAndSecretType(resources, secretType);
    }

    public void markResources(MultiClusterRotationMetadata metadata) {
        multiClusterRotationResourceRepository.save(new MultiClusterRotationResource(metadata.parentResourceCrn(), metadata.secretType(), PARENT_INITIAL));
        metadata.childResourceCrns().forEach(crn ->
                multiClusterRotationResourceRepository.save(new MultiClusterRotationResource(crn, metadata.secretType(), CHILD)));
    }
}
