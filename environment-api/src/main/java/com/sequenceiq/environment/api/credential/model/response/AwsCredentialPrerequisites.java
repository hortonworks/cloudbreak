package com.sequenceiq.environment.api.credential.model.response;

import static com.sequenceiq.environment.api.credential.doc.CredentialModelDescription.AWS_EXTERNAL_ID;
import static com.sequenceiq.environment.api.credential.doc.CredentialModelDescription.AWS_POLICY_JSON;

import java.io.Serializable;

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
}
