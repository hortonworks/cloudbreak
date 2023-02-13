package com.sequenceiq.environment.api.v1.environment.model.response;

import java.io.Serializable;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

public class FreeIpaImageResponse implements Serializable {

    @Schema(description = EnvironmentModelDescription.FREEIPA_INSTANCE_COUNT_BY_GROUP)
    private String catalog;

    @Schema(description = EnvironmentModelDescription.FREEIPA_IMAGE_ID)
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

    @Override
    public String toString() {
        return "FreeIpaImageResponse{" +
                "catalog='" + catalog + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
