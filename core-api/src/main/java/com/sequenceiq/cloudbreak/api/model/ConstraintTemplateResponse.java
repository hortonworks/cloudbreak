package com.sequenceiq.cloudbreak.api.model;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

@ApiModel
public class ConstraintTemplateResponse extends ConstraintTemplateBase {
    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.PUBLIC_IN_ACCOUNT, readOnly = true)
    private boolean publicInAccount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }
}
