package com.sequenceiq.cloudbreak.api.model.mpack;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.users.OrganizationResourceResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ManagementPackResponse extends ManagementPackBase {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(ModelDescriptions.PUBLIC_IN_ACCOUNT)
    private boolean publicInAccount;

    @ApiModelProperty(ModelDescriptions.ORGANIZATION_OF_THE_RESOURCE)
    private OrganizationResourceResponse organization;

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("public")
    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    public OrganizationResourceResponse getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationResourceResponse organization) {
        this.organization = organization;
    }
}
