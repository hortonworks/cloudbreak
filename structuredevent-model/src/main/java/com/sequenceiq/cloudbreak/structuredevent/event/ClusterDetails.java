package com.sequenceiq.cloudbreak.structuredevent.event;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterDetails {
    private Long id;

    private String name;

    private String description;

    private String status;

    private String statusReason;

    private Boolean secure;

    private String kerberosType;

    private Boolean gatewayEnabled;

    private String gatewayType;

    private String ssoType;

    private String ambariVersion;

    private String hdpVersion;

    private Set<String> hostGroups;

    private Boolean externalDatabase;

    private String databaseType;

    private String fileSystemType;

    private Boolean defaultFileSystem;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public Boolean getSecure() {
        return secure;
    }

    public void setSecure(Boolean secure) {
        this.secure = secure;
    }

    public String getKerberosType() {
        return kerberosType;
    }

    public void setKerberosType(String kerberosType) {
        this.kerberosType = kerberosType;
    }

    public Boolean getGatewayEnabled() {
        return gatewayEnabled;
    }

    public void setGatewayEnabled(Boolean gatewayEnabled) {
        this.gatewayEnabled = gatewayEnabled;
    }

    public String getGatewayType() {
        return gatewayType;
    }

    public void setGatewayType(String gatewayType) {
        this.gatewayType = gatewayType;
    }

    public String getSsoType() {
        return ssoType;
    }

    public void setSsoType(String ssoType) {
        this.ssoType = ssoType;
    }

    public String getAmbariVersion() {
        return ambariVersion;
    }

    public void setAmbariVersion(String ambariVersion) {
        this.ambariVersion = ambariVersion;
    }

    public String getHdpVersion() {
        return hdpVersion;
    }

    public void setHdpVersion(String hdpVersion) {
        this.hdpVersion = hdpVersion;
    }

    public Set<String> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<String> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public Boolean getExternalDatabase() {
        return externalDatabase;
    }

    public void setExternalDatabase(Boolean externalDatabase) {
        this.externalDatabase = externalDatabase;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public String getFileSystemType() {
        return fileSystemType;
    }

    public void setFileSystemType(String fileSystemType) {
        this.fileSystemType = fileSystemType;
    }

    public Boolean getDefaultFileSystem() {
        return defaultFileSystem;
    }

    public void setDefaultFileSystem(Boolean defaultFileSystem) {
        this.defaultFileSystem = defaultFileSystem;
    }
}
