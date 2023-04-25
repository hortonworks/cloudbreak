package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.base.DatabaseServerV4Base;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ModelDescriptions.DATABASE_SERVER_RESPONSE)
@JsonInclude(Include.NON_NULL)
public class DatabaseServerV4Response extends DatabaseServerV4Base {

    @ApiModelProperty(DatabaseServer.ID)
    private Long id;

    @ApiModelProperty(DatabaseServer.CRN)
    private String crn;

    @ApiModelProperty(value = DatabaseServer.DATABASE_VENDOR_DISPLAY_NAME, required = true)
    private String databaseVendorDisplayName;

    @ApiModelProperty(value = DatabaseServer.CONNECTION_DRIVER, required = true)
    private String connectionDriver;

    @ApiModelProperty(DatabaseServer.USERNAME)
    private SecretResponse connectionUserName;

    @ApiModelProperty(DatabaseServer.PASSWORD)
    private SecretResponse connectionPassword;

    @ApiModelProperty(DatabaseServer.CREATION_DATE)
    private Long creationDate;

    @ApiModelProperty(DatabaseServer.RESOURCE_STATUS)
    private ResourceStatus resourceStatus;

    @ApiModelProperty(DatabaseServer.STATUS)
    private Status status;

    @ApiModelProperty(DatabaseServer.STATUS_REASON)
    private String statusReason;

    @ApiModelProperty(DatabaseServer.SSL_CONFIG)
    private SslConfigV4Response sslConfig;

    @ApiModelProperty(DatabaseServer.CLUSTER_CRN)
    private String clusterCrn;

    @ApiModelProperty(DatabaseServer.MAJOR_VERSION)
    private MajorVersion majorVersion;

    @ApiModelProperty(value = DatabaseServer.DATABASE_PROPERTIES, required = true)
    private DatabasePropertiesV4Response databasePropertiesV4Response;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getDatabaseVendorDisplayName() {
        return databaseVendorDisplayName;
    }

    public void setDatabaseVendorDisplayName(String databaseVendorDisplayName) {
        this.databaseVendorDisplayName = databaseVendorDisplayName;
    }

    public String getConnectionDriver() {
        return connectionDriver;
    }

    public void setConnectionDriver(String connectionDriver) {
        this.connectionDriver = connectionDriver;
    }

    public SecretResponse getConnectionUserName() {
        return connectionUserName;
    }

    public void setConnectionUserName(SecretResponse connectionUserName) {
        this.connectionUserName = connectionUserName;
    }

    public SecretResponse getConnectionPassword() {
        return connectionPassword;
    }

    public void setConnectionPassword(SecretResponse connectionPassword) {
        this.connectionPassword = connectionPassword;
    }

    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    public ResourceStatus getResourceStatus() {
        return resourceStatus;
    }

    public Status getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setResourceStatus(ResourceStatus resourceStatus) {
        this.resourceStatus = resourceStatus;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public SslConfigV4Response getSslConfig() {
        return sslConfig;
    }

    public void setSslConfig(SslConfigV4Response sslConfig) {
        this.sslConfig = sslConfig;
    }

    public String getClusterCrn() {
        return clusterCrn;
    }

    public void setClusterCrn(String clusterCrn) {
        this.clusterCrn = clusterCrn;
    }

    public MajorVersion getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(MajorVersion majorVersion) {
        this.majorVersion = majorVersion;
    }

    public DatabasePropertiesV4Response getDatabasePropertiesV4Response() {
        return databasePropertiesV4Response;
    }

    public void setDatabasePropertiesV4Response(DatabasePropertiesV4Response databasePropertiesV4Response) {
        this.databasePropertiesV4Response = databasePropertiesV4Response;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DatabaseServerV4Response.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("crn='" + crn + "'")
                .add("databaseVendorDisplayName='" + databaseVendorDisplayName + "'")
                .add("connectionDriver='" + connectionDriver + "'")
                .add("connectionUserName=" + connectionUserName)
                .add("connectionPassword=" + connectionPassword)
                .add("creationDate=" + creationDate)
                .add("resourceStatus=" + resourceStatus)
                .add("status=" + status)
                .add("statusReason='" + statusReason + "'")
                .add("sslConfig=" + sslConfig)
                .add("clusterCrn='" + clusterCrn + "'")
                .add("majorVersion=" + majorVersion)
                .add("databasePropertiesV4Response=" + databasePropertiesV4Response)
                .toString();
    }
}
