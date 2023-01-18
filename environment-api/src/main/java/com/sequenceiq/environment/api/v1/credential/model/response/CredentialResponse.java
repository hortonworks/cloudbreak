package com.sequenceiq.environment.api.v1.credential.model.response;

import static com.sequenceiq.environment.api.doc.ModelDescriptions.CRN;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.credential.CredentialDescriptor;
import com.sequenceiq.environment.api.doc.credential.CredentialModelDescription;
import com.sequenceiq.environment.api.v1.credential.model.CredentialBase;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialResponseParameters;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = CredentialDescriptor.CREDENTIAL, allOf = CredentialBase.class, name = "CredentialV1Response")
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CredentialResponse extends CredentialBase {

    @Schema(description = ModelDescriptions.NAME, required = true)
    private String name;

    @Schema(description = CredentialModelDescription.ATTRIBUTES)
    private SecretResponse attributes;

    @Schema(description = CredentialModelDescription.AZURE_PARAMETERS)
    private AzureCredentialResponseParameters azure;

    @Schema(description = CRN)
    private String crn;

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    @Schema(description = ModelDescriptions.CREATOR)
    private String creator;

    @Schema(description = CredentialModelDescription.ACCOUNT_IDENTIFIER)
    private String accountId;

    @Schema(description = CredentialModelDescription.CREATED)
    private Long created;

    @Schema(description = CredentialModelDescription.CREDENTIAL_TYPE, required = true)
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
