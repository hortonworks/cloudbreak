package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.CHILD;
import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.PARENT_FINAL;
import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.PARENT_INITIAL;

import java.util.List;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationTrackingService;

@Service
public class SecretRotationExecutionDecisionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationExecutionDecisionProvider.class);

    @Inject
    private MultiClusterRotationTrackingService multiClusterRotationTrackingService;

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
                metadata.resourceCrn(), metadata.requestedExecutionType());
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
        return rotationMetadata -> rotationMetadata.multiClusterRotationMetadata()
                .map(multiClusterRotationMetadata ->
                        multiClusterRotationMetadata.childResourceCrns().contains(rotationMetadata.resourceCrn()) &&
                                resourcePresentInDb(rotationMetadata, CHILD))
                .orElseThrow(() -> new RuntimeException("Missing metadata for multi cluster rotation!"));
    }

    private Predicate<RotationMetadata> parentClusterInitialExecution() {
        return rotationMetadata -> rotationMetadata.multiClusterRotationMetadata()
                .map(multiClusterRotationMetadata ->
                        parentAndResourceMatches(rotationMetadata) &&
                                resourcePresentInDb(rotationMetadata, PARENT_INITIAL) &&
                                List.of(RotationFlowExecutionType.ROTATE, RotationFlowExecutionType.ROLLBACK).contains(rotationMetadata.currentExecution()))
                .orElseThrow(() -> new RuntimeException("Missing metadata for multi cluster rotation!"));
    }

    private Predicate<RotationMetadata> parentClusterFinalExecution() {
        return rotationMetadata -> rotationMetadata.multiClusterRotationMetadata()
                .map(multiClusterRotationMetadata ->
                        parentAndResourceMatches(rotationMetadata) &&
                                resourcePresentInDb(rotationMetadata, PARENT_FINAL) &&
                                rotationMetadata.currentExecution().equals(RotationFlowExecutionType.FINALIZE))
                .orElseThrow(() -> new RuntimeException("Missing metadata for multi cluster rotation!"));
    }

    private boolean resourcePresentInDb(RotationMetadata rotationMetadata, MultiClusterRotationResourceType type) {
        return multiClusterRotationTrackingService.multiRotationNeededForResource(rotationMetadata, type);
    }

    private boolean parentAndResourceMatches(RotationMetadata rotationMetadata) {
        return StringUtils.equals(rotationMetadata.multiClusterRotationMetadata().orElseThrow().parentResourceCrn(), rotationMetadata.resourceCrn());
    }
}
