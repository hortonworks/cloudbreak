package com.sequenceiq.cloudbreak.controller.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel
public class CredentialResponse extends CredentialBase {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;
    @ApiModelProperty(ModelDescriptions.PUBLIC_IN_ACCOUNT)
    private boolean publicInAccount;

    @JsonProperty("public")
    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
