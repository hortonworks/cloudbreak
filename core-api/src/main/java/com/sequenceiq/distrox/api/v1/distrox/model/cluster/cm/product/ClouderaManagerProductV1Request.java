package com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.product;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClouderaManagerProductV1Request implements JsonEntity {

    @NotNull
    @Schema(description = ModelDescriptions.ClouderaManagerProductDescription.NAME)
    private String name;

    @Schema(description = ModelDescriptions.ClouderaManagerProductDescription.VERSION)
    private String version;

    @Schema(description = ModelDescriptions.ClouderaManagerProductDescription.PARCEL)
    private String parcel;

    @Schema(description = ModelDescriptions.ClouderaManagerProductDescription.CSD)
    private List<String> csd;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getParcel() {
        return parcel;
    }

    public void setParcel(String parcel) {
        this.parcel = parcel;
    }

    public List<String> getCsd() {
        return csd;
    }

    public void setCsd(List<String> csd) {
        this.csd = csd;
    }

    public ClouderaManagerProductV1Request withName(String name) {
        this.name = name;
        return this;
    }

    public ClouderaManagerProductV1Request withVersion(String version) {
        this.version = version;
        return this;
    }

    public ClouderaManagerProductV1Request withParcel(String parcel) {
        this.parcel = parcel;
        return this;
    }

    public ClouderaManagerProductV1Request withCsd(List<String> csd) {
        this.csd = csd;
        return this;
    }

}
