package com.sequenceiq.environment.api.v1.credential.model.response;

import static com.sequenceiq.environment.api.doc.ModelDescriptions.CRN;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.authorization.resource.ResourceCrnAwareApiModel;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.environment.api.doc.credential.CredentialDescriptor;
import com.sequenceiq.environment.api.doc.credential.CredentialModelDescription;
import com.sequenceiq.environment.api.v1.credential.model.CredentialBase;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialResponseParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = CredentialDescriptor.CREDENTIAL, parent = CredentialBase.class, value = "CredentialV1Response")
@JsonInclude(Include.NON_NULL)
public class CredentialResponse extends CredentialBase implements ResourceCrnAwareApiModel {

    @ApiModelProperty(CredentialModelDescription.ATTRIBUTES)
    private SecretResponse attributes;

    @ApiModelProperty(CredentialModelDescription.AZURE_PARAMETERS)
    private AzureCredentialResponseParameters azure;

    @ApiModelProperty(CRN)
    private String crn;

    @ApiModelProperty(CredentialModelDescription.CREATOR)
    private String creator;

    @ApiModelProperty(CredentialModelDescription.CREATED)
    private Long created;

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

    @Override
    @JsonIgnore
    public String getResourceCrn() {
        return crn;
    }
}
