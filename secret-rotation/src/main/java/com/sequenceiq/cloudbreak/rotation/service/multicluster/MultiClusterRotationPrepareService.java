package com.sequenceiq.cloudbreak.rotation.service.multicluster;

import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.CHILD;
import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.PARENT_FINAL;
import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.PARENT_INITIAL;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.MultiClusterRotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResource;

@Service
public class MultiClusterRotationPrepareService {

    @Inject
    private Map<SecretType, RotationContextProvider> rotationContextProviderMap;

    @Inject
    private MultiClusterRotationTrackingService multiClusterRotationTrackingService;

    public void prepare(String resourceCrn, MultiSecretType multiSecretType) {
        MultiClusterRotationMetadata metadata = getMultiClusterRotationContextProvider(resourceCrn, multiSecretType)
                .getMultiClusterRotationMetadata(resourceCrn);
        Set<MultiClusterRotationResource> multiClusterRotationResources = multiClusterRotationTrackingService.getAll(metadata.allResources(), multiSecretType);
        validateMultiRotationInput(multiSecretType, multiClusterRotationResources);
        if (StringUtils.equals(metadata.parentResourceCrn(), resourceCrn)) {
            prepareParentRotation(metadata, multiClusterRotationResources);
        } else if (metadata.childResourceCrns().contains(resourceCrn)) {
            validateChildRotation(resourceCrn, multiClusterRotationResources);
        } else {
            throw new CloudbreakServiceException("Resource is not present in multi rotation metadata, context provider should be fixed.");
        }
    }

    private void validateMultiRotationInput(MultiSecretType multiSecretType, Set<MultiClusterRotationResource> multiClusterRotationResources) {
        if (multiClusterRotationResources.stream()
                .anyMatch(multiRotationResource -> !multiRotationResource.getSecretType().equals(multiSecretType))) {
            throw new BadRequestException("There is another ongoing multi cluster rotation for the resource or its parent, thus rotation is not possible.");
        }
    }

    private void prepareParentRotation(MultiClusterRotationMetadata metadata, Set<MultiClusterRotationResource> multiClusterRotationResources) {
        if (multiClusterRotationResources.size() == 0) {
            multiClusterRotationTrackingService.markResources(metadata);
        } else if (multiClusterRotationResources.stream().anyMatch(multiRotationResource -> CHILD.equals(multiRotationResource.getType()))) {
            throw new BadRequestException(String.format("There is at least one child for parent %s, where multi rotation is not finished",
                    metadata.parentResourceCrn()));
        } else if (multiClusterRotationResources.stream().noneMatch(multiRotationResource -> PARENT_FINAL.equals(multiRotationResource.getType()))) {
            throw new CloudbreakServiceException("Parent multi rotation should be finished, but no record for it.");
        }
    }

    private void validateChildRotation(String resourceCrn, Set<MultiClusterRotationResource> multiClusterRotationResources) {
        if (multiClusterRotationResources.size() == 0) {
            throw new BadRequestException("There is no ongoing multi rotation for parent of the resource.");
        } else if (multiClusterRotationResources.stream().anyMatch(multiRotationResource -> PARENT_INITIAL.equals(multiRotationResource.getType()))) {
            throw new BadRequestException("You should wait for parent resource to finish the initial rotation.");
        } else if (multiClusterRotationResources.stream().noneMatch(multiRotationResource ->
                StringUtils.equals(multiRotationResource.getResourceCrn(), resourceCrn))) {
            throw new BadRequestException(String.format("For ongoing multi cluster rotation the child %s is already rotated.", resourceCrn));
        }
    }

    private MultiClusterRotationContextProvider getMultiClusterRotationContextProvider(String resourceCrn, MultiSecretType multiSecretType) {
        RotationContextProvider rotationContextProvider = rotationContextProviderMap.get(multiSecretType.getSecretTypeByResourceCrn(resourceCrn));
        if (rotationContextProvider instanceof MultiClusterRotationContextProvider) {
            return (MultiClusterRotationContextProvider) rotationContextProvider;
        } else {
            throw new RuntimeException("There is no metadata for multi cluster secret rotation!");
        }
    }
}
