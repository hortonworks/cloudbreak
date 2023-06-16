package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ImageModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class StackImageV4Response implements JsonEntity {
    @ApiModelProperty(ImageModelDescription.IMAGE_NAME)
    private String name;

    @ApiModelProperty(ImageModelDescription.IMAGE_CATALOG_URL)
    private String catalogUrl;

    @ApiModelProperty(ImageModelDescription.IMAGE_ID)
    private String id;

    @ApiModelProperty(ImageModelDescription.IMAGE_CATALOG_NAME)
    private String catalogName;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.IMAGE_OS)
    private String os;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCatalogUrl() {
        return catalogUrl;
    }

    public void setCatalogUrl(String catalogUrl) {
        this.catalogUrl = catalogUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }
}
