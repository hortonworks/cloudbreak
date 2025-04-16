package com.sequenceiq.cloudbreak.cloud.response;

import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.AWS_EXTERNAL_ID;
import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.AWS_POLICY_JSON;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class AwsCredentialPrerequisites extends CredentialBasePrerequisites implements Serializable {

    @Schema(description = AWS_EXTERNAL_ID, requiredMode = Schema.RequiredMode.REQUIRED)
    private String externalId;

    @Schema(description = AWS_POLICY_JSON, requiredMode = Schema.RequiredMode.REQUIRED)
    private String policyJson;

    public AwsCredentialPrerequisites() {
    }

    public AwsCredentialPrerequisites(String externalId, String policyJson) {
        this.externalId = externalId;
        this.policyJson = policyJson;
    }

    public AwsCredentialPrerequisites(String externalId, String policyJson, Map<String, String> policies, Set<GranularPolicyResponse> granularPolicies) {
        this.externalId = externalId;
        this.policyJson = policyJson;
        setPolicies(policies);
        setGranularPolicies(granularPolicies);
    }

    public String getExternalId() {
        return externalId;
    }

    public String getPolicyJson() {
        return policyJson;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setPolicyJson(String policyJson) {
        this.policyJson = policyJson;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AwsCredentialPrerequisites that = (AwsCredentialPrerequisites) o;
        return Objects.equals(externalId, that.externalId) && Objects.equals(policyJson, that.policyJson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(externalId, policyJson);
    }

    @Override
    public String toString() {
        return "AwsCredentialPrerequisites{" +
                "externalId='" + externalId + '\'' +
                ", policyJson='" + policyJson + '\'' +
                '}';
    }

}
