package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ImageModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class StackImageV4Response implements JsonEntity {
    @Schema(description = ImageModelDescription.IMAGE_NAME)
    private String name;

    @Schema(description = ImageModelDescription.IMAGE_CATALOG_URL)
    private String catalogUrl;

    @Schema(description = ImageModelDescription.IMAGE_ID)
    private String id;

    @Schema(description = ImageModelDescription.IMAGE_CATALOG_NAME)
    private String catalogName;

    @Schema(description = ModelDescriptions.StackModelDescription.IMAGE_OS)
    private String os;

    @Schema(description = ModelDescriptions.ARCHITECTURE)
    private String architecture;

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

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }
}
