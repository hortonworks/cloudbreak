package com.sequenceiq.distrox.api.v1.distrox.model.image;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DistroXImageV1Request implements Serializable {

    @Schema(description = StackModelDescription.IMAGE_CATALOG)
    private String catalog;

    @Schema(description = StackModelDescription.IMAGE_ID)
    private String id;

    @Schema(description = StackModelDescription.IMAGE_OS)
    private String os;

    @Schema(description = StackModelDescription.IMAGE_ARCHITECTURE)
    private String architecture;

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

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }
}
