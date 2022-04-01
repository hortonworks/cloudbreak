package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxChangeImageCatalogRequest {

    @Size(max = 100, min = 5, message = "The length of the image catalog has to be in range of 5 to 100")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "Image catalog can only contain lowercase alphanumeric characters and hyphens and has to start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ImageModelDescription.IMAGE_CATALOG_NAME, required = true)
    private String imageCatalog;

    public String getImageCatalog() {
        return imageCatalog;
    }

    public void setImageCatalog(String imageCatalog) {
        this.imageCatalog = imageCatalog;
    }

    @Override
    public String toString() {
        return "SdxChangeImageCatalogRequest{" +
                "imageCatalog='" + imageCatalog + '\'' +
                '}';
    }
}
