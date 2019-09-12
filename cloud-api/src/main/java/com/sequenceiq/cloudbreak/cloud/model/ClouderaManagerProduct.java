package com.sequenceiq.cloudbreak.cloud.model;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClouderaManagerProduct implements Serializable {

    private String name;

    private String version;

    private String parcel;

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

    public ClouderaManagerProduct withName(String name) {
        this.name = name;
        return this;
    }

    public ClouderaManagerProduct withVersion(String version) {
        this.version = version;
        return this;
    }

    public ClouderaManagerProduct withParcel(String parcel) {
        this.parcel = parcel;
        return this;
    }

    public ClouderaManagerProduct withCsd(List<String> csd) {
        this.csd = csd;
        return this;
    }
}
