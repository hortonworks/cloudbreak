package com.sequenceiq.cloudbreak.rotation.service;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;

public record RotationMetadata(
        SecretType secretType,
        RotationFlowExecutionType currentExecution,
        RotationFlowExecutionType requestedExecutionType,
        String resourceCrn,
        Optional<MultiSecretType> multiSecretType,
        Map<String, String> additionalProperties
) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private SecretType secretType;

        private RotationFlowExecutionType currentExecution;

        private RotationFlowExecutionType requestedExecutionType;

        private String resourceCrn;

        private Optional<MultiSecretType> multiSecretType = Optional.empty();

        private Map<String, String> additionalProperties = Maps.newHashMap();

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

        public Builder multiSecretType(MultiSecretType multiSecretType) {
            this.multiSecretType = Optional.of(multiSecretType);
            return this;
        }

        public Builder additionalProperties(Map<String, String> additionalProperties) {
            this.additionalProperties = additionalProperties;
            return this;
        }

        public RotationMetadata build() {
            return new RotationMetadata(secretType, currentExecution, requestedExecutionType, resourceCrn, multiSecretType, additionalProperties);
        }
    }
}
