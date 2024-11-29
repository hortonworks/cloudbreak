package com.sequenceiq.environment.api.v1.credential.model;

import java.io.Serializable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.credential.CredentialModelDescription;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.GcpCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.mock.MockParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.OpenstackParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.yarn.YarnParameters;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(subTypes = {CredentialRequest.class, CredentialResponse.class})
public abstract class CredentialBase implements Serializable {

    @NotNull
    @Schema(description = CredentialModelDescription.CLOUD_PLATFORM, requiredMode = Schema.RequiredMode.REQUIRED)
    private String cloudPlatform;

    @Valid
    @Schema(description = CredentialModelDescription.AWS_PARAMETERS)
    private AwsCredentialParameters aws;

    @Valid
    @Schema(description = CredentialModelDescription.GCP_PARAMETERS)
    private GcpCredentialParameters gcp;

    @Valid
    @Schema(description = CredentialModelDescription.OPENSTACK_PARAMETERS_DEPRECATED)
    @Deprecated
    private OpenstackParameters openstack;

    @Valid
    @Schema(description = CredentialModelDescription.YARN_PARAMETERS)
    private YarnParameters yarn;

    @Valid
    @Schema(hidden = true)
    private MockParameters mock;

    @Size(max = 1000)
    @Schema(description = ModelDescriptions.DESCRIPTION)
    private String description;

    @Schema(description = CredentialModelDescription.VERIFICATION_STATUS_TEXT)
    private String verificationStatusText;

    @Schema(description = CredentialModelDescription.VERIFY_PERMISSIONS)
    private boolean verifyPermissions;

    @Schema(description = CredentialModelDescription.SKIP_ORG_POLICY_DECISIONS)
    private boolean skipOrgPolicyDecisions;

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

    public boolean isSkipOrgPolicyDecisions() {
        return skipOrgPolicyDecisions;
    }

    public void setSkipOrgPolicyDecisions(boolean skipOrgPolicyDecisions) {
        this.skipOrgPolicyDecisions = skipOrgPolicyDecisions;
    }

    @Override
    public String toString() {
        return "CredentialBase{" +
                "cloudPlatform='" + cloudPlatform + '\'' +
                ", aws=" + aws +
                ", gcp=" + gcp +
                ", yarn=" + yarn +
                ", mock=" + mock +
                ", description='" + description + '\'' +
                ", verificationStatusText='" + verificationStatusText + '\'' +
                ", verifyPermissions=" + verifyPermissions +
                ", skipOrgPolicyDecisions=" + skipOrgPolicyDecisions +
                '}';
    }
}
