package com.sequenceiq.cloudbreak.rotation.service;

import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SecretRotationExecutionDecisionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationExecutionDecisionProvider.class);

    public boolean executionRequired(RotationMetadata metadata) {
        Predicate<RotationMetadata> executionTypeCorrect = emptyExecution().or(explicitExecution());
        if (!executionTypeCorrect.test(metadata)) {
            logRotationMetadata("Execution type is not empty and not explicit. Execution is not needed.", metadata);
            return false;
        }
        logRotationMetadata("Execution is required for single cluster execution.", metadata);
        return true;
    }

    private Predicate<RotationMetadata> explicitExecution() {
        return rotationMetadata -> rotationMetadata.currentExecution().equals(rotationMetadata.requestedExecutionType());
    }

    private Predicate<RotationMetadata> emptyExecution() {
        return rotationMetadata -> rotationMetadata.requestedExecutionType() == null;
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
