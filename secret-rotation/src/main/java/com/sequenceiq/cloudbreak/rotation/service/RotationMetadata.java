package com.sequenceiq.cloudbreak.rotation.service;

import java.util.Optional;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationMetadata;

public record RotationMetadata(
        SecretType secretType,
        RotationFlowExecutionType currentExecution,
        RotationFlowExecutionType requestedExecutionType,
        String resourceCrn,
        Optional<MultiClusterRotationMetadata> multiClusterRotationMetadata
) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private SecretType secretType;

        private RotationFlowExecutionType currentExecution;

        private RotationFlowExecutionType requestedExecutionType;

        private String resourceCrn;

        private Optional<MultiClusterRotationMetadata> multiClusterRotationMetadata = Optional.empty();

        public Builder secretType(SecretType secretType) {
            this.secretType = secretType;
            return this;
        }

        public Builder currentExecution(RotationFlowExecutionType currentExecution) {
            this.currentExecution = currentExecution;
            return this;
        }

        public Builder requestedExecutionType(RotationFlowExecutionType requestedExecutionType) {
            this.requestedExecutionType = requestedExecutionType;
            return this;
        }

        public Builder resourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public Builder multiClusterRotationMetadata(MultiClusterRotationMetadata multiClusterRotationMetadata) {
            this.multiClusterRotationMetadata = Optional.of(multiClusterRotationMetadata);
            return this;
        }

        public RotationMetadata build() {
            return new RotationMetadata(secretType, currentExecution, requestedExecutionType, resourceCrn, multiClusterRotationMetadata);
        }
    }
}
