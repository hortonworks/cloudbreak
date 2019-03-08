package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClouderaManagerProductV4Response implements JsonEntity {

    @NotNull
    @ApiModelProperty(ModelDescriptions.ClouderaManagerProductDescription.NAME)
    private String name;

    @ApiModelProperty(ModelDescriptions.ClouderaManagerProductDescription.VERSION)
    private String version;

    @ApiModelProperty(ModelDescriptions.ClouderaManagerProductDescription.PARCEL)
    private String parcel;

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

    public ClouderaManagerProductV4Response withName(String name) {
        setName(name);
        return this;
    }

    public ClouderaManagerProductV4Response withVersion(String version) {
        setVersion(version);
        return this;
    }

    public ClouderaManagerProductV4Response withParcel(String parcel) {
        setParcel(parcel);
        return this;
    }
}
