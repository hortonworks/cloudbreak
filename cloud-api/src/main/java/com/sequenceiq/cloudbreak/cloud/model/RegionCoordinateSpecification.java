package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.domain.CdpSupportedServices;

public class RegionCoordinateSpecification {
    @JsonProperty("name")
    private String name;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("latitude")
    private String latitude;

    @JsonProperty("longitude")
    private String longitude;

    @JsonProperty("k8sSupported")
    private boolean k8sSupported;

    @JsonProperty("entitlements")
    private List<String> entitlements;

    @JsonProperty("defaultDbVmtype")
    private String defaultDbVmtype;

    @JsonProperty("defaultArmDbVmtype")
    private String defaultArmDbVmtype;

    @JsonProperty("cdpSupportedServices")
    private Set<CdpSupportedServices> cdpSupportedServices = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isK8sSupported() {
        return k8sSupported;
    }

    public void setK8sSupported(boolean k8sSupported) {
        this.k8sSupported = k8sSupported;
    }

    public List<String> getEntitlements() {
        return entitlements;
    }

    public void setEntitlements(List<String> entitlements) {
        this.entitlements = entitlements;
    }

    public String getDefaultDbVmtype() {
        return defaultDbVmtype;
    }

    public void setDefaultDbVmtype(String defaultDbVmtype) {
        this.defaultDbVmtype = defaultDbVmtype;
    }

    public String getDefaultArmDbVmtype() {
        return defaultArmDbVmtype;
    }

    public void setDefaultArmDbVmtype(String defaultArmDbVmtype) {
        this.defaultArmDbVmtype = defaultArmDbVmtype;
    }

    public Set<CdpSupportedServices> getCdpSupportedServices() {
        return cdpSupportedServices;
    }

    public void setCdpSupportedServices(Set<CdpSupportedServices> cdpSupportedServices) {
        this.cdpSupportedServices = cdpSupportedServices;
    }
}
