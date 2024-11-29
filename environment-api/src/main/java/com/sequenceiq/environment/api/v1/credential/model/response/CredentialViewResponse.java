package com.sequenceiq.environment.api.v1.credential.model.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.credential.CredentialDescriptor;
import com.sequenceiq.environment.api.doc.credential.CredentialModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = CredentialDescriptor.CREDENTIAL_VIEW, name = "CredentialViewV1Response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialViewResponse implements Serializable {

    @Schema(description = ModelDescriptions.NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = ModelDescriptions.CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String crn;

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    @Schema(description = ModelDescriptions.CREATOR)
    private String creator;

    @Schema(description = CredentialModelDescription.CLOUD_PLATFORM, requiredMode = Schema.RequiredMode.REQUIRED)
    private String cloudPlatform;

    @Schema(description = ModelDescriptions.DESCRIPTION)
    private String description;

    @Schema(description = CredentialModelDescription.VERIFICATION_STATUS_TEXT)
    private String verificationStatusText;

    @Schema(description = CredentialModelDescription.CREDENTIAL_TYPE)
    private CredentialType type;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean govCloud = Boolean.FALSE;

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

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    public String getCreator() {
        return creator;
    }

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
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
