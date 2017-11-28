package com.sequenceiq.cloudbreak.api.model.imagecatalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageCatalogResponse extends ImageCatalogBase {

    @ApiModelProperty(value = ModelDescriptions.ID, required = true)
    private Long id;

    @ApiModelProperty(value = ModelDescriptions.PUBLIC_IN_ACCOUNT, required = true)
    private boolean publicInAccount;

    //CHECKSTYLE:OFF
    @ApiModelProperty(value = ModelDescriptions.ImageCatalogDescription.DEFAULT, required = true)
    private boolean isDefault;
    //CHECKSTYLE:ON

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

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean b) {
        this.isDefault = b;
    }
}
