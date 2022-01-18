package com.sequenceiq.environment.api.v1.credential.model.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.credential.CredentialDescriptor;
import com.sequenceiq.environment.api.doc.credential.CredentialModelDescription;
import com.sequenceiq.common.model.CredentialType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = CredentialDescriptor.CREDENTIAL_VIEW, value = "CredentialViewV1Response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialViewResponse implements Serializable {

    @ApiModelProperty(ModelDescriptions.NAME)
    private String name;

    @ApiModelProperty(ModelDescriptions.CRN)
    private String crn;

    @ApiModelProperty(CredentialModelDescription.CREATOR)
    private String creator;

    @ApiModelProperty(value = CredentialModelDescription.CLOUD_PLATFORM, required = true)
    private String cloudPlatform;

    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(CredentialModelDescription.VERIFICATION_STATUS_TEXT)
    private String verificationStatusText;

    @ApiModelProperty(CredentialModelDescription.CREDENTIAL_TYPE)
    private CredentialType type;

    private Boolean govCloud;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVerificationStatusText() {
        return verificationStatusText;
    }

    public void setVerificationStatusText(String verificationStatusText) {
        this.verificationStatusText = verificationStatusText;
    }

    public CredentialType getType() {
        return type;
    }

    public void setType(CredentialType type) {
        this.type = type;
    }

    public Boolean getGovCloud() {
        return govCloud;
    }

    public void setGovCloud(Boolean govCloud) {
        this.govCloud = govCloud;
    }

    @Override
    public String toString() {
        return "CredentialViewResponse{" +
                "name='" + name + '\'' +
                ", crn='" + crn + '\'' +
                ", creator='" + creator + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", description='" + description + '\'' +
                ", verificationStatusText='" + verificationStatusText + '\'' +
                ", type=" + type +
                ", govCloud=" + govCloud +
                '}';
    }
}
