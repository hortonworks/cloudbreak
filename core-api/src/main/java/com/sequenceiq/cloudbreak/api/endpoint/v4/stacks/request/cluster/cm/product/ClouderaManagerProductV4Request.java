package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProductBase;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClouderaManagerProductV4Request implements JsonEntity, ClouderaManagerProductBase {

    @NotEmpty
    @Schema(description = ModelDescriptions.ClouderaManagerProductDescription.NAME)
    private String name;

    @NotEmpty
    @Schema(description = ModelDescriptions.ClouderaManagerProductDescription.VERSION)
    private String version;

    @NotEmpty
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

    public ClouderaManagerProductV4Request withName(String name) {
        this.name = name;
        return this;
    }

    public ClouderaManagerProductV4Request withVersion(String version) {
        this.version = version;
        return this;
    }

    public ClouderaManagerProductV4Request withParcel(String parcel) {
        this.parcel = parcel;
        return this;
    }

    public ClouderaManagerProductV4Request withCsd(List<String> csd) {
        this.csd = csd;
        return this;
    }

    @Override
    public String toString() {
        return "ClouderaManagerProductV4Request{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", parcel='" + parcel + '\'' +
                ", csd=" + csd +
                '}';
    }
}
