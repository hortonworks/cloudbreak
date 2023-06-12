package com.sequenceiq.environment.api.v1.environment.model.response;

import java.io.Serializable;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class FreeIpaImageResponse implements Serializable {

    @ApiModelProperty(EnvironmentModelDescription.FREEIPA_INSTANCE_COUNT_BY_GROUP)
    private String catalog;

    @ApiModelProperty(EnvironmentModelDescription.FREEIPA_IMAGE_ID)
    private String id;

    @ApiModelProperty(EnvironmentModelDescription.FREEIPA_IMAGE_OS_TYPE)
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
        return "FreeIpaImageResponse{" +
                "catalog='" + catalog + '\'' +
                ", id='" + id + '\'' +
                ", os='" + os + '\'' +
                '}';
    }
}
