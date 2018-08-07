package com.sequenceiq.cloudbreak.api.model.v2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ImageSettings implements JsonEntity {
    @ApiModelProperty(StackModelDescription.IMAGE_CATALOG)
    private String imageCatalog;

    @ApiModelProperty(StackModelDescription.IMAGE_ID)
    private String imageId;

    @ApiModelProperty(StackModelDescription.OS_TYPE)
    private String os;

    public String getImageCatalog() {
        return imageCatalog;
    }

    public void setImageCatalog(String imageCatalog) {
        this.imageCatalog = imageCatalog;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }
}
