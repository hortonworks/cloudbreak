package com.sequenceiq.cloudbreak.cloud.response;

import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.COMPONENT;
import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.COMPONENT_POLICY;
import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.POLICY_NAME;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public record GranularPolicyResponse(@Schema(description = COMPONENT, requiredMode = REQUIRED) String component,
        @Schema(description = POLICY_NAME, requiredMode = REQUIRED) String name,
        @Schema(description = COMPONENT_POLICY, requiredMode = REQUIRED) String policy) {

    @Override
    public String policy() {
        return policy;
    }

    @Override
    public String component() {
        return component;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return "GranularPolicyResponse{" +
                "policy='" + policy + '\'' +
                ", component=" + component +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GranularPolicyResponse(String component1, String name1, String policy1))) {
            return false;
        }
        return Objects.equals(name, name1) && Objects.equals(policy, policy1) && Objects.equals(component, component1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(component, name, policy);
    }
}
