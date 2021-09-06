package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ImageSettingsV4Request implements JsonEntity {

    @ApiModelProperty(StackModelDescription.IMAGE_CATALOG)
    private String catalog;

    @ApiModelProperty(StackModelDescription.IMAGE_ID)
    private String id;

    @ApiModelProperty(StackModelDescription.OS_TYPE)
    private String os;

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    @Override
    public String toString() {
        return "ImageSettingsV4Request{" +
                "catalog='" + catalog + '\'' +
                ", id='" + id + '\'' +
                ", os='" + os + '\'' +
                '}';
    }
}
