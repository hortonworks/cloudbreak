package com.sequenceiq.cloudbreak.rotation.service;

import java.util.Map;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;

public record RotationMetadata(
        SecretType secretType,
        RotationFlowExecutionType currentExecution,
        RotationFlowExecutionType requestedExecutionType,
        String resourceCrn,
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

        public Builder additionalProperties(Map<String, String> additionalProperties) {
            this.additionalProperties = additionalProperties;
            return this;
        }

        public RotationMetadata build() {
            return new RotationMetadata(secretType, currentExecution, requestedExecutionType, resourceCrn, additionalProperties);
        }
    }
}
