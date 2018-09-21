package com.sequenceiq.cloudbreak.api.model.v3.credential;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.CredentialModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class CredentialPrerequisites implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    private String cloudPlatform;

    @ApiModelProperty(value = CredentialModelDescription.ACCOUNT_IDENTIFIER, required = true)
    private String accountId;

    @ApiModelProperty(value = CredentialModelDescription.AWS_CREDENTIAL_PREREQUISITES, required = true)
    private AwsCredentialPrerequisites aws;

    public CredentialPrerequisites(String cloudPlatform, String accountId, AwsCredentialPrerequisites aws) {
        this.cloudPlatform = cloudPlatform;
        this.accountId = accountId;
        this.aws = aws;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public String getAccountId() {
        return accountId;
    }

    public AwsCredentialPrerequisites getAws() {
        return aws;
    }
}
