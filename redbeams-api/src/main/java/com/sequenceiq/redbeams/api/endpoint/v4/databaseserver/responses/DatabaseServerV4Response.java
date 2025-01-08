package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.base.DatabaseServerV4Base;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.DATABASE_SERVER_RESPONSE)
@JsonInclude(Include.NON_NULL)
public class DatabaseServerV4Response extends DatabaseServerV4Base {

    @Schema(description = DatabaseServer.ID)
    private Long id;

    @Schema(description = DatabaseServer.CRN)
    private String crn;

    @Schema(description = DatabaseServer.DATABASE_VENDOR_DISPLAY_NAME, required = true)
    private String databaseVendorDisplayName;

    @Schema(description = DatabaseServer.CONNECTION_DRIVER, required = true)
    private String connectionDriver;

    @Schema(description = DatabaseServer.USERNAME)
    private SecretResponse connectionUserName;

    @Schema(description = DatabaseServer.PASSWORD)
    private SecretResponse connectionPassword;

    @Schema(description = DatabaseServer.CREATION_DATE)
    private Long creationDate;

    @Schema(description = DatabaseServer.RESOURCE_STATUS)
    private ResourceStatus resourceStatus;

    @Schema(description = DatabaseServer.STATUS)
    private Status status;

    @Schema(description = DatabaseServer.STATUS_REASON)
    private String statusReason;

    @Schema(description = DatabaseServer.SSL_CONFIG)
    private SslConfigV4Response sslConfig;

    @Schema(description = DatabaseServer.CLUSTER_CRN)
    private String clusterCrn;

    @Schema(description = DatabaseServer.MAJOR_VERSION)
    private MajorVersion majorVersion;

    @Schema(description = DatabaseServer.DATABASE_PROPERTIES, required = true)
    private DatabasePropertiesV4Response databasePropertiesV4Response;

    @Schema(description = DatabaseServer.INSTANCE_TYPE)
    private String instanceType;

    @Schema(description = DatabaseServer.STORAGE_SIZE)
    private Long storageSize;

    @Schema(description = DatabaseServer.CANARY_DATABASE_PROPERTIES)
    private CanaryDatabasePropertiesV4Response canaryDatabasePropertiesV4Response;

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

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public Long getStorageSize() {
        return storageSize;
    }

    public void setStorageSize(Long storageSize) {
        this.storageSize = storageSize;
    }

    public CanaryDatabasePropertiesV4Response getCanaryDatabasePropertiesV4Response() {
        return canaryDatabasePropertiesV4Response;
    }

    public void setCanaryDatabasePropertiesV4Response(CanaryDatabasePropertiesV4Response canaryDatabasePropertiesV4Response) {
        this.canaryDatabasePropertiesV4Response = canaryDatabasePropertiesV4Response;
    }

    @Override
    public String toString() {
        return "DatabaseServerV4Response{" +
                "id=" + id +
                ", crn='" + crn + '\'' +
                ", databaseVendorDisplayName='" + databaseVendorDisplayName + '\'' +
                ", connectionDriver='" + connectionDriver + '\'' +
                ", connectionUserName=" + connectionUserName +
                ", connectionPassword=" + connectionPassword +
                ", creationDate=" + creationDate +
                ", resourceStatus=" + resourceStatus +
                ", status=" + status +
                ", statusReason='" + statusReason + '\'' +
                ", sslConfig=" + sslConfig +
                ", clusterCrn='" + clusterCrn + '\'' +
                ", majorVersion=" + majorVersion +
                ", databasePropertiesV4Response=" + databasePropertiesV4Response +
                ", instanceType='" + instanceType + '\'' +
                ", storageSize=" + storageSize +
                ", canaryDatabasePropertiesV4Response=" + canaryDatabasePropertiesV4Response +
                '}';
    }
}