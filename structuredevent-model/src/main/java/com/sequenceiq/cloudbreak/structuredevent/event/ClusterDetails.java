package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.structuredevent.json.AnonymizingBase64Serializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterDetails implements Serializable {
    private Long id;

    private String name;

    private String description;

    private String status;

    @JsonSerialize(using = AnonymizingBase64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String statusReason;

    private Boolean gatewayEnabled;

    private String gatewayType;

    private String ssoType;

    private String clusterType;

    private String clusterVersion;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> hostGroups = new ArrayList<>();

    private Boolean externalDatabase;

    private String databaseType;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<RdsDetails> databases = new ArrayList<>();

    private String fileSystemType;

    private Long creationStarted;

    private Long creationFinished;

    private Long upSince;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean razEnabled;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean rmsEnabled;

    private Boolean dbSslEnabled;

    public Boolean isDbSslEnabled() {
        return dbSslEnabled;
    }

    public void setDbSslEnabled(Boolean dbSslEnabled) {
        this.dbSslEnabled = dbSslEnabled;
    }

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

    public String getClusterType() {
        return clusterType;
    }

    public void setClusterType(String clusterType) {
        this.clusterType = clusterType;
    }

    public String getClusterVersion() {
        return clusterVersion;
    }

    public void setClusterVersion(String clusterVersion) {
        this.clusterVersion = clusterVersion;
    }

    public List<String> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(List<String> hostGroups) {
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

    public List<RdsDetails> getDatabases() {
        return databases;
    }

    public void setDatabases(List<RdsDetails> databases) {
        this.databases = databases;
    }

    public String getFileSystemType() {
        return fileSystemType;
    }

    public void setFileSystemType(String fileSystemType) {
        this.fileSystemType = fileSystemType;
    }

    public Long getCreationStarted() {
        return creationStarted;
    }

    public void setCreationStarted(Long creationStarted) {
        this.creationStarted = creationStarted;
    }

    public Long getCreationFinished() {
        return creationFinished;
    }

    public void setCreationFinished(Long creationFinished) {
        this.creationFinished = creationFinished;
    }

    public Long getUpSince() {
        return upSince;
    }

    public void setUpSince(Long upSince) {
        this.upSince = upSince;
    }

    public boolean isRazEnabled() {
        return razEnabled;
    }

    public void setRazEnabled(boolean razEnabled) {
        this.razEnabled = razEnabled;
    }

    public boolean isRmsEnabled() {
        return rmsEnabled;
    }

    public void setRmsEnabled(boolean rmsEnabled) {
        this.rmsEnabled = rmsEnabled;
    }
}
