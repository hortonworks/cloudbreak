package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.INITIATED_PARENT;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.InterServiceMultiClusterRotationService;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationService;

@Service
public class SecretRotationExecutionDecisionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationExecutionDecisionProvider.class);

    @Inject
    private MultiClusterRotationService multiClusterRotationService;

    @Inject
    private Optional<InterServiceMultiClusterRotationService> interServiceMultiClusterRotationTrackingService;

    public boolean executionRequired(RotationMetadata metadata) {
        Predicate<RotationMetadata> executionTypeCorrect = emptyExecution().or(explicitExecution());
        if (!executionTypeCorrect.test(metadata)) {
            logRotationMetadata("Execution type is not empty and not explicit. Execution is not needed.", metadata);
            return false;
        }
        if (singleClusterExecution(metadata)) {
            logRotationMetadata("Execution is required for single cluster execution.", metadata);
            return true;
        }
        if (multiClusterExecution(metadata)) {
            if (childClusterExecution(metadata)) {
                logRotationMetadata("Child cluster execution is required.", metadata);
                return true;
            } else if (parentClusterInitialExecution(metadata)) {
                logRotationMetadata("Parent cluster initial execution is required.", metadata);
                return true;
            } else if (parentClusterFinalExecution(metadata)) {
                logRotationMetadata("Parent cluster final execution is required.", metadata);
                return true;
            }
        }
        logRotationMetadata("Execution is not required.", metadata);
        return false;
    }

    private Predicate<RotationMetadata> explicitExecution() {
        return rotationMetadata -> rotationMetadata.currentExecution().equals(rotationMetadata.requestedExecutionType());
    }

    private Predicate<RotationMetadata> emptyExecution() {
        return rotationMetadata -> rotationMetadata.requestedExecutionType() == null;
    }

    private boolean singleClusterExecution(RotationMetadata rotationMetadata) {
        return !rotationMetadata.secretType().multiSecret();
    }

    private boolean multiClusterExecution(RotationMetadata rotationMetadata) {
        return rotationMetadata.secretType().multiSecret();
    }

    private boolean childClusterExecution(RotationMetadata metadata) {
        return metadata.multiSecretType().orElseThrow().getChildrenCrnDescriptors().contains(
                CrnResourceDescriptor.getByCrnString(metadata.resourceCrn()));
    }

    private boolean parentClusterInitialExecution(RotationMetadata metadata) {
        return metadata.multiSecretType().orElseThrow().getParentCrnDescriptor().equals(CrnResourceDescriptor.getByCrnString(metadata.resourceCrn()))
                && !resourcePresentInDb(metadata, INITIATED_PARENT) && List.of(PREVALIDATE, ROTATE, ROLLBACK).contains(metadata.currentExecution());
    }

    private boolean parentClusterFinalExecution(RotationMetadata metadata) {
        return metadata.multiSecretType().orElseThrow().getParentCrnDescriptor().equals(CrnResourceDescriptor.getByCrnString(metadata.resourceCrn()))
                && resourcePresentInDb(metadata, INITIATED_PARENT) && !childPresent(metadata) && metadata.currentExecution().equals(FINALIZE);
    }

    private boolean resourcePresentInDb(RotationMetadata metadata, MultiClusterRotationResourceType type) {
        return multiClusterRotationService.getMultiRotationEntryForMetadata(metadata, type).isPresent();
    }

    private boolean childPresent(RotationMetadata metadata) {
        MultiSecretType multiSecretType = metadata.multiSecretType().orElseThrow();
        return interServiceMultiClusterRotationTrackingService.map(interServiceMultiClusterRotationService ->
                interServiceMultiClusterRotationService.checkOngoingChildrenMultiSecretRotations(metadata.resourceCrn(), multiSecretType)).orElse(false);
    }

    private void logRotationMetadata(String message, RotationMetadata rotationMetadata) {
        LOGGER.info("{} Current execution {}, resource {}, secret type: {}, requested execution type: {}",
                message,
                rotationMetadata.currentExecution(),
                rotationMetadata.resourceCrn(),
                rotationMetadata.secretType(),
                rotationMetadata.requestedExecutionType());
    }
}
