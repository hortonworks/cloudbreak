package com.sequenceiq.environment.credential.attributes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.credential.attributes.aws.AwsCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.gcp.GcpCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.mock.MockCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.yarn.YarnCredentialAttributes;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CredentialAttributes {

    private AwsCredentialAttributes aws;

    private AzureCredentialAttributes azure;

    private GcpCredentialAttributes gcp;

    private YarnCredentialAttributes yarn;

    private MockCredentialAttributes mock;

    public AwsCredentialAttributes getAws() {
        return aws;
    }

    public void setAws(AwsCredentialAttributes aws) {
        this.aws = aws;
    }

    public AzureCredentialAttributes getAzure() {
        return azure;
    }

    public void setAzure(AzureCredentialAttributes azure) {
        this.azure = azure;
    }

    public GcpCredentialAttributes getGcp() {
        return gcp;
    }

    public void setGcp(GcpCredentialAttributes gcp) {
        this.gcp = gcp;
    }

    public YarnCredentialAttributes getYarn() {
        return yarn;
    }

    public void setYarn(YarnCredentialAttributes yarn) {
        this.yarn = yarn;
    }

    public MockCredentialAttributes getMock() {
        return mock;
    }

    public void setMock(MockCredentialAttributes mock) {
        this.mock = mock;
    }
}
