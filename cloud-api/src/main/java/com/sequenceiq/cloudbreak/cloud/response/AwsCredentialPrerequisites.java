package com.sequenceiq.cloudbreak.cloud.response;

import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.AWS_EXTERNAL_ID;
import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.AWS_POLICY_JSON;

import java.io.Serializable;
import java.util.Objects;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class AwsCredentialPrerequisites implements Serializable {

    @ApiModelProperty(value = AWS_EXTERNAL_ID, required = true)
    private String externalId;

    @ApiModelProperty(value = AWS_POLICY_JSON, required = true)
    private String policyJson;

    public AwsCredentialPrerequisites(String externalId, String policyJson) {
        this.externalId = externalId;
        this.policyJson = policyJson;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getPolicyJson() {
        return policyJson;
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
