package com.sequenceiq.environment.api.v1.environment.model.request;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeIpaImageRequest {

    @ApiModelProperty(value = EnvironmentModelDescription.FREEIPA_IMAGE_CATALOG)
    @Size(max = 255)
    private String catalog;

    @ApiModelProperty(EnvironmentModelDescription.FREEIPA_IMAGE_ID)
    @Size(max = 255)
    private String id;

    @Override
    public String toString() {
        return "FreeIpaImageRequest{" +
                "catalog=" + catalog +
                ", id=" + id +
                '}';
    }

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
}
