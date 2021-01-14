package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.DatabaseServerModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class StackDatabaseServerResponse {

    @ApiModelProperty(DatabaseServerModelDescription.CRN)
    private String crn;

    @ApiModelProperty(DatabaseServerModelDescription.NAME)
    private String name;

    @ApiModelProperty(DatabaseServerModelDescription.DESCRIPTION)
    private String description;

    @ApiModelProperty(DatabaseServerModelDescription.ENVIRONMENT_CRN)
    private String environmentCrn;

    @ApiModelProperty(DatabaseServerModelDescription.HOST)
    private String host;

    @ApiModelProperty(DatabaseServerModelDescription.PORT)
    private Integer port;

    @ApiModelProperty(DatabaseServerModelDescription.DATABASE_VENDOR)
    private String databaseVendor;

    @ApiModelProperty(DatabaseServerModelDescription.DATABASE_VENDOR_DISPLAY_NAME)
    private String databaseVendorDisplayName;

    @ApiModelProperty(DatabaseServerModelDescription.CREATION_DATE)
    private Long creationDate;

    @ApiModelProperty(DatabaseServerModelDescription.RESOURCE_STATUS)
    private DatabaseServerResourceStatus resourceStatus;

    @ApiModelProperty(DatabaseServerModelDescription.STATUS)
    private DatabaseServerStatus status;

    @ApiModelProperty(DatabaseServerModelDescription.STATUS_REASON)
    private String statusReason;

    @ApiModelProperty(DatabaseServerModelDescription.CLUSTER_CRN)
    private String clusterCrn;

    @ApiModelProperty(DatabaseServerModelDescription.SSL_CONFIG)
    private DatabaseServerSslConfig sslConfig;

    public StackDatabaseServerResponse() {
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
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

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDatabaseVendor() {
        return databaseVendor;
    }

    public void setDatabaseVendor(String databaseVendor) {
        this.databaseVendor = databaseVendor;
    }

    public String getDatabaseVendorDisplayName() {
        return databaseVendorDisplayName;
    }

    public void setDatabaseVendorDisplayName(String databaseVendorDisplayName) {
        this.databaseVendorDisplayName = databaseVendorDisplayName;
    }

    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    public DatabaseServerResourceStatus getResourceStatus() {
        return resourceStatus;
    }

    public void setResourceStatus(DatabaseServerResourceStatus resourceStatus) {
        this.resourceStatus = resourceStatus;
    }

    public DatabaseServerStatus getStatus() {
        return status;
    }

    public void setStatus(DatabaseServerStatus status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getClusterCrn() {
        return clusterCrn;
    }

    public void setClusterCrn(String clusterCrn) {
        this.clusterCrn = clusterCrn;
    }

    public DatabaseServerSslConfig getSslConfig() {
        return sslConfig;
    }

    public void setSslConfig(DatabaseServerSslConfig sslConfig) {
        this.sslConfig = sslConfig;
    }

    @Override
    public String toString() {
        return "StackDatabaseServerResponse{" +
                "crn='" + crn + '\'' +
                ", name='" + name + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", clusterCrn='" + clusterCrn + '\'' +
                '}';
    }
}

