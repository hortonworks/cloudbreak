package com.sequenceiq.environment.api.v1.credential.model;

import java.io.Serializable;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.credential.CredentialModelDescription;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.GcpCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.mock.MockParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.OpenstackParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.yarn.YarnParameters;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(subTypes = {CredentialRequest.class, CredentialResponse.class})
public abstract class CredentialBase implements Serializable {

    @NotNull
    @ApiModelProperty(value = CredentialModelDescription.CLOUD_PLATFORM, required = true)
    private String cloudPlatform;

    @Valid
    @ApiModelProperty(CredentialModelDescription.AWS_PARAMETERS)
    private AwsCredentialParameters aws;

    @Valid
    @ApiModelProperty(CredentialModelDescription.GCP_PARAMETERS)
    private GcpCredentialParameters gcp;

    @Valid
    @ApiModelProperty(CredentialModelDescription.OPENSTACK_PARAMETERS)
    private OpenstackParameters openstack;

    @Valid
    @ApiModelProperty(CredentialModelDescription.YARN_PARAMETERS)
    private YarnParameters yarn;

    @Valid
    @ApiModelProperty(hidden = true)
    private MockParameters mock;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(CredentialModelDescription.VERIFICATION_STATUS_TEXT)
    private String verificationStatusText;

    @ApiModelProperty(CredentialModelDescription.VERIFY_PERMISSIONS)
    private boolean verifyPermissions;

    public MockParameters getMock() {
        return mock;
    }

    public void setMock(MockParameters mock) {
        this.mock = mock;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public AwsCredentialParameters getAws() {
        return aws;
    }

    public void setAws(AwsCredentialParameters aws) {
        this.aws = aws;
    }

    public GcpCredentialParameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpCredentialParameters gcp) {
        this.gcp = gcp;
    }

    public OpenstackParameters getOpenstack() {
        return openstack;
    }

    public void setOpenstack(OpenstackParameters openstack) {
        this.openstack = openstack;
    }

    public YarnParameters getYarn() {
        return yarn;
    }

    public void setYarn(YarnParameters yarn) {
        this.yarn = yarn;
    }

    public String getVerificationStatusText() {
        return verificationStatusText;
    }

    public void setVerificationStatusText(String verificationStatusText) {
        this.verificationStatusText = verificationStatusText;
    }

    public boolean isVerifyPermissions() {
        return verifyPermissions;
    }

    public void setVerifyPermissions(boolean verifyPermissions) {
        this.verifyPermissions = verifyPermissions;
    }

    @Override
    public String toString() {
        return "CredentialBase{" +
                "cloudPlatform='" + cloudPlatform + '\'' +
                ", aws=" + aws +
                ", gcp=" + gcp +
                ", openstack=" + openstack +
                ", yarn=" + yarn +
                ", mock=" + mock +
                ", description='" + description + '\'' +
                ", verificationStatusText='" + verificationStatusText + '\'' +
                ", verifyPermissions=" + verifyPermissions +
                '}';
    }
}
