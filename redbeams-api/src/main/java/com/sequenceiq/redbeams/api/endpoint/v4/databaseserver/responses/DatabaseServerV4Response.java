package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

// import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;
// import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.base.DatabaseServerV4Base;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;
// import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RDSConfigModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(Include.NON_NULL)
public class DatabaseServerV4Response extends DatabaseServerV4Base {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    // FIXME
    // @ApiModelProperty(ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    // private WorkspaceResourceV4Response workspace;

    // @ApiModelProperty(RDSConfigModelDescription.CLUSTER_NAMES)
    // private Set<String> clusterNames;

    @ApiModelProperty(value = DatabaseServer.DATABASE_VENDOR_DISPLAY_NAME, required = true)
    private String databaseVendorDisplayName;

    @ApiModelProperty(value = DatabaseServer.CONNECTION_DRIVER, required = true)
    private String connectionDriver;

    @ApiModelProperty(DatabaseServer.CONNECTION_USER_NAME)
    private SecretV4Response connectionUserName;

    @ApiModelProperty(DatabaseServer.CONNECTION_PASSWORD)
    private SecretV4Response connectionPassword;

    @ApiModelProperty(ModelDescriptions.CREATION_DATE)
    private Long creationDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public SecretV4Response getConnectionUserName() {
        return connectionUserName;
    }

    public void setConnectionUserName(SecretV4Response connectionUserName) {
        this.connectionUserName = connectionUserName;
    }

    public SecretV4Response getConnectionPassword() {
        return connectionPassword;
    }

    public void setConnectionPassword(SecretV4Response connectionPassword) {
        this.connectionPassword = connectionPassword;
    }

    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    // public Set<String> getClusterNames() {
    //     return clusterNames;
    // }

    // public void setClusterNames(Set<String> clusterNames) {
    //     this.clusterNames = clusterNames;
    // }

    // public WorkspaceResourceV4Response getWorkspace() {
    //     return workspace;
    // }

    // public void setWorkspace(WorkspaceResourceV4Response workspace) {
    //     this.workspace = workspace;
    // }
}
