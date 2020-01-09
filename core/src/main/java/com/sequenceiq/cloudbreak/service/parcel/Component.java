package com.sequenceiq.cloudbreak.service.parcel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Component {

    private String name;

    @JsonProperty("pkg_release")
    private String pkgRelease;

    @JsonProperty("pkg_version")
    private String pkgVersion;

    private String version;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPkgRelease() {
        return pkgRelease;
    }

    public void setPkgRelease(String pkgRelease) {
        this.pkgRelease = pkgRelease;
    }

    public String getPkgVersion() {
        return pkgVersion;
    }

    public void setPkgVersion(String pkgVersion) {
        this.pkgVersion = pkgVersion;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
