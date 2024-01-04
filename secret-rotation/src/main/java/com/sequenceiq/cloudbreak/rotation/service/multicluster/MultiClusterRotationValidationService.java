package com.sequenceiq.cloudbreak.rotation.service.multicluster;

import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.INITIATED_PARENT;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResource;

@Service
public class MultiClusterRotationValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiClusterRotationValidationService.class);

    @Inject
    private Map<SecretType, RotationContextProvider> rotationContextProviderMap;

    @Inject
    private MultiClusterRotationService multiClusterRotationService;

    @Inject
    private Optional<InterServiceMultiClusterRotationService> interServiceMultiClusterRotationTrackingService;

    public void validateMultiRotationRequest(String resourceCrn, SecretType inputSecretType) {
        inputSecretType.getMultiSecretType().ifPresent(inputMultiSecretType -> {
            CrnResourceDescriptor crnResourceDescriptor = CrnResourceDescriptor.getByCrnString(resourceCrn);
            Set<MultiClusterRotationResource> multiClusterRotationResources = multiClusterRotationService
                    .getMultiRotationEntriesForResource(inputMultiSecretType, resourceCrn);
            if (crnResourceDescriptor.equals(inputMultiSecretType.getParentCrnDescriptor())) {
                validateParentRotation(resourceCrn, multiClusterRotationResources, inputSecretType);
            } else if (inputMultiSecretType.getChildrenCrnDescriptors().contains(crnResourceDescriptor)) {
                validateChildRotation(resourceCrn, inputSecretType, multiClusterRotationResources);
            } else {
                throw new CloudbreakServiceException("Type of resource is not matching with the given secret type.");
            }
        });
    }

    private void validateParentRotation(String resourceCrn, Set<MultiClusterRotationResource> multiClusterRotationResources, SecretType secretType) {
        if (noOngoingMultiRotation(multiClusterRotationResources)) {
            LOGGER.info("Rotation for parent resource {} is ready to be started for multi-cluster secret type {}.", resourceCrn, secretType);
        } else if (parentRotationInitiatedAndChildrenStillPending(resourceCrn, multiClusterRotationResources, secretType)) {
            throw new BadRequestException(
                    String.format("Rotation for parent resource %s cannot be finalized for multi-cluster secret type %s, " +
                            "since multi-cluster secret rotation for children is still needed.", resourceCrn, secretType));
        }
    }

    private boolean parentRotationInitiatedAndChildrenStillPending(String resourceCrn, Set<MultiClusterRotationResource> multiClusterRotationResources,
            SecretType secretType) {
        return multiClusterRotationResources.stream().anyMatch(multiRotationResource -> INITIATED_PARENT.equals(multiRotationResource.getType()))
                && interServiceMultiClusterRotationTrackingService.isPresent()
                && interServiceMultiClusterRotationTrackingService.get().checkOngoingChildrenMultiSecretRotations(resourceCrn,
                secretType.getMultiSecretType().orElseThrow());
    }

    private void validateChildRotation(String resourceCrn, SecretType inputMultiSecretType,
            Set<MultiClusterRotationResource> multiClusterRotationResources) {
        if (noOngoingMultiRotation(multiClusterRotationResources)) {
            throw new BadRequestException(String.format("Resource %s is not marked as rotatable cluster for multi-cluster secret type %s, " +
                    "thus rotation not needed.", resourceCrn, inputMultiSecretType));
        }
    }

    private static boolean noOngoingMultiRotation(Set<MultiClusterRotationResource> multiClusterRotationResources) {
        return multiClusterRotationResources.isEmpty();
    }
}
