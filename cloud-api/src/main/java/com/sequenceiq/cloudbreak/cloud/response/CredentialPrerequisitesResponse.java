package com.sequenceiq.cloudbreak.cloud.response;

import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.ACCOUNT_IDENTIFIER;
import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.AWS_CREDENTIAL_PREREQUISITES;
import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.AZURE_CREDENTIAL_PREREQUISITES;
import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.CLOUD_PLATFORM;
import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.GCP_CREDENTIAL_PREREQUISITES;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class CredentialPrerequisitesResponse implements Serializable {

    public CredentialPrerequisitesResponse() {
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setAws(AwsCredentialPrerequisites aws) {
        this.aws = aws;
    }

    public void setAzure(AzureCredentialPrerequisites azure) {
        this.azure = azure;
    }

    public void setGcp(GcpCredentialPrerequisites gcp) {
        this.gcp = gcp;
    }

    @ApiModelProperty(value = CLOUD_PLATFORM, required = true)
    private String cloudPlatform;

    @ApiModelProperty(ACCOUNT_IDENTIFIER)
    private String accountId;

    @ApiModelProperty(AWS_CREDENTIAL_PREREQUISITES)
    private AwsCredentialPrerequisites aws;

    @ApiModelProperty(AZURE_CREDENTIAL_PREREQUISITES)
    private AzureCredentialPrerequisites azure;

    @ApiModelProperty(GCP_CREDENTIAL_PREREQUISITES)
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
