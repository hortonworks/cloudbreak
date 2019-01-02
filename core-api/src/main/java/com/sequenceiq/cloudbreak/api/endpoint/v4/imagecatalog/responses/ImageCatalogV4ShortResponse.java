package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.base.ImageCatalogV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ImageCatalogDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(Include.NON_NULL)
public class ImageCatalogV4ShortResponse extends ImageCatalogV4Base {

    @ApiModelProperty(value = ModelDescriptions.ID, required = true)
    private Long id;

    @ApiModelProperty(value = ImageCatalogDescription.DEFAULT, required = true)
    private boolean usedAsDefault;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isUsedAsDefault() {
        return usedAsDefault;
    }

    public void setUsedAsDefault(boolean usedAsDefault) {
        this.usedAsDefault = usedAsDefault;
    }

}
