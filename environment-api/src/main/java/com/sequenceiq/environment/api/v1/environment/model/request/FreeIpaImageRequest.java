package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeIpaImageRequest implements Serializable {

    @Schema(description = EnvironmentModelDescription.FREEIPA_IMAGE_CATALOG)
    @Size(max = 255)
    private String catalog;

    @Schema(description = EnvironmentModelDescription.FREEIPA_IMAGE_ID)
    @Size(max = 255)
    private String id;

    @Schema(description = EnvironmentModelDescription.FREEIPA_IMAGE_OS_TYPE)
    @Size(max = 255)
    private String os;

    @Override
    public String toString() {
        return "FreeIpaImageRequest{" +
                "catalog='" + catalog + '\'' +
                ", id='" + id + '\'' +
                ", os='" + os + '\'' +
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

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }
}
