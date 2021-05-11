package com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.CustomImageCatalogDescription.DESCRIPTION;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.CustomImageCatalogDescription.NAME;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class CustomImageCatalogV4CreateRequest {

    @Size(max = 255, min = 1, message = "The length of the ID must be between 1 and 255")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = NAME, required = true)
    private String name;

    @Size(max = 255, message = "The length of the description must be less than 255")
    @ApiModelProperty(DESCRIPTION)
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
