package com.sequenceiq.environment.credential.attributes;

import com.sequenceiq.environment.credential.attributes.aws.AwsCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.cumulus.CumulusYarnCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.gcp.GcpCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.mock.MockCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.openstack.OpenStackCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.yarn.YarnCredentialAttributes;

public class CredentialAttributes {

    private AwsCredentialAttributes aws;

    private AzureCredentialAttributes azure;

    private GcpCredentialAttributes gcp;

    private OpenStackCredentialAttributes openstack;

    private YarnCredentialAttributes yarn;

    private CumulusYarnCredentialAttributes cumulus;

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

    public OpenStackCredentialAttributes getOpenstack() {
        return openstack;
    }

    public void setOpenstack(OpenStackCredentialAttributes openstack) {
        this.openstack = openstack;
    }

    public YarnCredentialAttributes getYarn() {
        return yarn;
    }

    public void setYarn(YarnCredentialAttributes yarn) {
        this.yarn = yarn;
    }

    public CumulusYarnCredentialAttributes getCumulus() {
        return cumulus;
    }

    public void setCumulus(CumulusYarnCredentialAttributes cumulus) {
        this.cumulus = cumulus;
    }

    public MockCredentialAttributes getMock() {
        return mock;
    }

    public void setMock(MockCredentialAttributes mock) {
        this.mock = mock;
    }
}
