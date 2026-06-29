package com.sequenceiq.cloudbreak.rotation;

import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class RotationNodeValidationService {

    @Inject
    private StackDtoService stackDtoService;

    public void validateNoStoppedInstances(String resourceCrn, SecretType secretType) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        validateNoStoppedInstances(stack, secretType);
    }

    public void validateNoStoppedInstances(StackDto stack, SecretType secretType) {
        List<String> stoppedInstances = stack.getNotDeletedInstanceMetaData().stream()
                .filter(InstanceMetadataView::isStopped)
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .filter(Objects::nonNull)
                .toList();
        if (!stoppedInstances.isEmpty()) {
            throw new SecretRotationException(String.format(
                    "There are stopped instances in the cluster, '%s' rotation cannot be performed. " +
                            "Please start all stopped nodes before retrying. Stopped instances: %s",
                    secretType, stoppedInstances));
        }
    }
}
