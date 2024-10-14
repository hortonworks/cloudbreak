package com.sequenceiq.cloudbreak.rotation.service.multicluster;

import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.INITIATED_PARENT;
import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.PENDING_CHILD;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResource;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType;
import com.sequenceiq.cloudbreak.rotation.repository.MultiClusterRotationResourceRepository;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@Service
public class MultiClusterRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiClusterRotationService.class);

    @Inject
    private MultiClusterRotationResourceRepository multiClusterRotationResourceRepository;

    @Inject
    private Optional<InterServiceMultiClusterRotationService> interServiceMultiClusterRotationTrackingService;

    public Set<MultiClusterRotationResource> getMultiRotationEntriesForResource(MultiSecretType secretType, String resourceCrn) {
        return multiClusterRotationResourceRepository.findAllBySecretTypeAndResourceCrn(secretType, resourceCrn);
    }

    public Set<MultiClusterRotationResource> getMultiRotationChildEntriesForResource(String resourceCrn) {
        return multiClusterRotationResourceRepository.findAllByResourceCrnAndType(resourceCrn, PENDING_CHILD);
    }

    public Set<MultiClusterRotationResource> getMultiRotationEntriesForSecretAndResources(MultiSecretType secretType, Set<String> crns) {
        return multiClusterRotationResourceRepository.findAllBySecretTypeAndResourceCrnIn(secretType, crns);
    }

    public Optional<MultiClusterRotationResource> getMultiRotationEntryForMetadata(RotationMetadata metadata, MultiClusterRotationResourceType type) {
        return multiClusterRotationResourceRepository.findByResourceCrnAndSecretTypeAndType(metadata.resourceCrn(),
                metadata.multiSecretType().orElseThrow(), type);
    }

    public void deleteAllByCrn(String crn) {
        multiClusterRotationResourceRepository.deleteAllByResourceCrn(crn);
    }

    public void updateMultiRotationEntriesAfterRotate(RotationMetadata metadata) {
        metadata.multiSecretType().ifPresent(secretType -> {
            CrnResourceDescriptor crnResourceDescriptor = CrnResourceDescriptor.getByCrnString(metadata.resourceCrn());
            if (crnResourceDescriptor.equals(secretType.getParentCrnDescriptor())) {
                Optional<MultiClusterRotationResource> multiClusterRotationResource = getMultiRotationEntryForMetadata(metadata, INITIATED_PARENT);
                if (multiClusterRotationResource.isPresent()) {
                    throw new SecretRotationException("Multi rotation resource should not present for parent after rotate.", null);
                } else {
                    addMultiRotationEntriesAfterFirstParentRotation(metadata, secretType);
                }
            }
        });
    }

    public void updateMultiRotationEntriesAfterFinalize(RotationMetadata metadata) {
        metadata.multiSecretType().ifPresent(secretType -> {
            CrnResourceDescriptor crnResourceDescriptor = CrnResourceDescriptor.getByCrnString(metadata.resourceCrn());
            if (crnResourceDescriptor.equals(secretType.getParentCrnDescriptor())) {
                LOGGER.info("Removing entry from database for parent resource {}, which means second phase of it's multi rotation is finished",
                        metadata.resourceCrn());
                getMultiRotationEntryForMetadata(metadata, INITIATED_PARENT).ifPresent(multiClusterRotationResourceRepository::delete);
            } else if (secretType.getChildrenCrnDescriptors().contains(crnResourceDescriptor)) {
                LOGGER.info("Removing entry from database for child resource {}, which means it's multi rotation is finished", metadata.resourceCrn());
                getMultiRotationEntryForMetadata(metadata, PENDING_CHILD).ifPresent(multiClusterRotationResourceRepository::delete);
            }
        });
    }

    public void markChildrenMultiRotationEntriesLocally(Set<String> crns, String multiSecret) {
        LOGGER.info("Adding entries from database for child resources {}.", crns);
        MultiSecretType multiSecretType = MultiSecretType.valueOf(multiSecret);
        crns.forEach(crn -> multiClusterRotationResourceRepository.save(new MultiClusterRotationResource(crn, multiSecretType, PENDING_CHILD)));
    }

    private void addMultiRotationEntriesAfterFirstParentRotation(RotationMetadata metadata, MultiSecretType secretType) {
        LOGGER.info("Adding entry to database for parent resource {}, which means first phase of it's multi rotation is finished",
                metadata.resourceCrn());
        multiClusterRotationResourceRepository.save(new MultiClusterRotationResource(metadata.resourceCrn(), secretType, INITIATED_PARENT));
        LOGGER.info("Calling service of child resources to add entries for multi rotation of children regarding parent resource {}", metadata.resourceCrn());
        interServiceMultiClusterRotationTrackingService.ifPresent(interServiceTracking ->
                interServiceTracking.markChildren(metadata.resourceCrn(), secretType));
    }
}
