package com.sequenceiq.environment.api.v1.environment.model.response;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import io.swagger.annotations.ApiModelProperty;

public class FreeIpaImageResponse {

    @ApiModelProperty(EnvironmentModelDescription.FREEIPA_INSTANCE_COUNT_BY_GROUP)
    private String catalog;

    @ApiModelProperty(EnvironmentModelDescription.FREEIPA_IMAGE_ID)
    private String id;

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
