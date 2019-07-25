package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

// import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.base.DatabaseServerV4Base;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;
import com.sequenceiq.redbeams.doc.ModelDescriptions.RedbeamsManagedDatabaseServer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

// import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;

@ApiModel
@JsonInclude(Include.NON_NULL)
public class DatabaseServerV4Response extends DatabaseServerV4Base {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(ModelDescriptions.CRN)
    private String crn;

    @ApiModelProperty(value = DatabaseServer.DATABASE_VENDOR_DISPLAY_NAME, required = true)
    private String databaseVendorDisplayName;

    @ApiModelProperty(value = DatabaseServer.CONNECTION_DRIVER, required = true)
    private String connectionDriver;

    @ApiModelProperty(DatabaseServer.CONNECTION_USER_NAME)
    private SecretResponse connectionUserName;

    @ApiModelProperty(DatabaseServer.CONNECTION_PASSWORD)
    private SecretResponse connectionPassword;

    @ApiModelProperty(ModelDescriptions.CREATION_DATE)
    private Long creationDate;

    @ApiModelProperty(ModelDescriptions.RESOURCE_STATUS)
    private ResourceStatus resourceStatus;

    @ApiModelProperty(RedbeamsManagedDatabaseServer.STATUS)
    private Status status;

    @ApiModelProperty(RedbeamsManagedDatabaseServer.STATUS_REASON)
    private String statusReason;

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
}
