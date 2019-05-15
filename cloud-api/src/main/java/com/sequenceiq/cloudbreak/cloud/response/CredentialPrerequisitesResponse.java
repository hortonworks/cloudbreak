package com.sequenceiq.cloudbreak.cloud.response;

import java.io.Serializable;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.AwsCredentialPrerequisites;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.AzureCredentialPrerequisites;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.GcpCredentialPrerequisites;
import com.sequenceiq.cloudbreak.api.model.annotations.Immutable;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.CredentialModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Immutable
@ApiModel
public class CredentialPrerequisitesResponse implements Serializable {

    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    private String cloudPlatform;

    @ApiModelProperty(CredentialModelDescription.ACCOUNT_IDENTIFIER)
    private String accountId;

    @ApiModelProperty(CredentialModelDescription.AWS_CREDENTIAL_PREREQUISITES)
    private AwsCredentialPrerequisites aws;

    @ApiModelProperty(CredentialModelDescription.AZURE_CREDENTIAL_PREREQUISITES)
    private AzureCredentialPrerequisites azure;

    @ApiModelProperty(CredentialModelDescription.GCP_CREDENTIAL_PREREQUISITES)
    private GcpCredentialPrerequisites gcp;

    public CredentialPrerequisitesResponse(String cloudPlatform, String accountId, AwsCredentialPrerequisites aws) {
        this.cloudPlatform = cloudPlatform;
        this.accountId = accountId;
        this.aws = aws;
    }

    public CredentialPrerequisitesResponse(String cloudPlatform, AzureCredentialPrerequisites azure) {
        this.cloudPlatform = cloudPlatform;
        this.azure = azure;
    }

    public CredentialPrerequisitesResponse(String cloudPlatform, GcpCredentialPrerequisites gcp) {
        this.cloudPlatform = cloudPlatform;
        this.gcp = gcp;
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

    public AzureCredentialPrerequisites getAzure() {
        return azure;
    }

    public GcpCredentialPrerequisites getGcp() {
        return gcp;
    }
}
