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
        Predicate<RotationMetadata> multiClusterExecution = multiClusterExecution().and(executionTypeCorrect);
        Predicate<RotationMetadata> singleClusterExecution = singleClusterExecution().and(executionTypeCorrect);
        boolean result = singleClusterExecution
                .or(multiClusterExecution.and(childClusterExecution()))
                .or(multiClusterExecution.and(parentClusterInitialExecution()))
                .or(multiClusterExecution.and(parentClusterFinalExecution()))
                .test(metadata);
        LOGGER.info("Execution of current rotation phase {} is {} based on resource:[{}], secret type:[{}] and requested execution type:[{}].",
                metadata.currentExecution(), result ? "needed" : "not needed", metadata.resourceCrn(),
                metadata.secretType(), metadata.requestedExecutionType());
        return result;
    }

    private Predicate<RotationMetadata> explicitExecution() {
        return rotationMetadata -> rotationMetadata.currentExecution().equals(rotationMetadata.requestedExecutionType());
    }

    private Predicate<RotationMetadata> emptyExecution() {
        return rotationMetadata -> rotationMetadata.requestedExecutionType() == null;
    }

    private Predicate<RotationMetadata> singleClusterExecution() {
        return rotationMetadata -> !rotationMetadata.secretType().multiSecret();
    }

    private Predicate<RotationMetadata> multiClusterExecution() {
        return rotationMetadata -> rotationMetadata.secretType().multiSecret();
    }

    private Predicate<RotationMetadata> childClusterExecution() {
        return metadata -> metadata.multiSecretType().orElseThrow().getChildrenCrnDescriptors().contains(
                CrnResourceDescriptor.getByCrnString(metadata.resourceCrn()));
    }

    private Predicate<RotationMetadata> parentClusterInitialExecution() {
        return metadata ->
                metadata.multiSecretType().orElseThrow().getParentCrnDescriptor().equals(CrnResourceDescriptor.getByCrnString(metadata.resourceCrn()))
                && !resourcePresentInDb(metadata, INITIATED_PARENT) && List.of(PREVALIDATE, ROTATE, ROLLBACK).contains(metadata.currentExecution());
    }

    private Predicate<RotationMetadata> parentClusterFinalExecution() {
        return metadata ->
                metadata.multiSecretType().orElseThrow().getParentCrnDescriptor().equals(CrnResourceDescriptor.getByCrnString(metadata.resourceCrn()))
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
}
