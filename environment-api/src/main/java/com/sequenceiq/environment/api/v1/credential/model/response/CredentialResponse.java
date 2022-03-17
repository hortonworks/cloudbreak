package com.sequenceiq.environment.api.v1.credential.model.response;

import static com.sequenceiq.environment.api.doc.ModelDescriptions.CRN;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.credential.CredentialDescriptor;
import com.sequenceiq.environment.api.doc.credential.CredentialModelDescription;
import com.sequenceiq.environment.api.v1.credential.model.CredentialBase;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialResponseParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = CredentialDescriptor.CREDENTIAL, parent = CredentialBase.class, value = "CredentialV1Response")
@JsonInclude(Include.NON_NULL)
public class CredentialResponse extends CredentialBase {

    @ApiModelProperty(value = ModelDescriptions.NAME, required = true, allowableValues = "length range[5, 100]")
    private String name;

    @ApiModelProperty(CredentialModelDescription.ATTRIBUTES)
    private SecretResponse attributes;

    @ApiModelProperty(CredentialModelDescription.AZURE_PARAMETERS)
    private AzureCredentialResponseParameters azure;

    @ApiModelProperty(CRN)
    private String crn;

    @ApiModelProperty(CredentialModelDescription.CREATOR)
    private String creator;

    @ApiModelProperty(CredentialModelDescription.ACCOUNT_IDENTIFIER)
    private String accountId;

    @ApiModelProperty(CredentialModelDescription.CREATED)
    private Long created;

    @ApiModelProperty(value = CredentialModelDescription.CREDENTIAL_TYPE, required = true)
    private CredentialType type;

    private Boolean govCloud;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public AzureCredentialResponseParameters getAzure() {
        return azure;
    }

    public void setAzure(AzureCredentialResponseParameters azure) {
        this.azure = azure;
    }

    public SecretResponse getAttributes() {
        return attributes;
    }

    public void setAttributes(SecretResponse attributes) {
        this.attributes = attributes;
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

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "CredentialResponse{" +
                "name='" + name + '\'' +
                ", azure=" + azure +
                ", crn='" + crn + '\'' +
                ", creator='" + creator + '\'' +
                ", accountId='" + accountId + '\'' +
                ", created=" + created +
                ", govCloud=" + govCloud +
                ", type=" + type +
                '}';
    }
}
