package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClouderaManagerProduct implements JsonEntity {

    private String name;

    private String version;

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

    public ClouderaManagerProduct withName(String name) {
        setName(name);
        return this;
    }

    public ClouderaManagerProduct withVersion(String version) {
        setVersion(version);
        return this;
    }

    public ClouderaManagerProduct withParcel(String parcel) {
        setParcel(parcel);
        return this;
    }

}
