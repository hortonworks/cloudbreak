package com.sequenceiq.cloudbreak.api.model.v3.credential;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.CredentialModelDescription.AWS_EXTERNAL_ID;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.CredentialModelDescription.AWS_POLICY_JSON;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class AwsCredentialPrerequisites implements JsonEntity {

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
}
