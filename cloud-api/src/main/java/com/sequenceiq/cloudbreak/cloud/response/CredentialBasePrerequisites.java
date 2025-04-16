package com.sequenceiq.cloudbreak.cloud.response;

import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.GRANULAR_POLICIES;
import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.POLICIES;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

public class CredentialBasePrerequisites {

    @Schema(description = POLICIES, requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> policies = new LinkedHashMap<>();

    @Schema(description = GRANULAR_POLICIES, requiredMode = NOT_REQUIRED)
    private Set<GranularPolicyResponse> granularPolicies = new HashSet<>();

    public Set<GranularPolicyResponse> getGranularPolicies() {
        return granularPolicies;
    }

    public void setGranularPolicies(Set<GranularPolicyResponse> granularPolicies) {
        this.granularPolicies = granularPolicies;
    }

    public Map<String, String> getPolicies() {
        return policies;
    }

    public void setPolicies(Map<String, String> policies) {
        this.policies = policies;
    }

    @Override
    public String toString() {
        return "CredentialBasePrerequisites{" +
                "policies=" + policies +
                ", granularPolicies=" + granularPolicies +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CredentialBasePrerequisites that) {
            return Objects.equals(policies, that.policies) && Objects.equals(granularPolicies, that.granularPolicies);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(policies, granularPolicies);
    }

}
