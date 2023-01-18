package com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.Database;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RDSConfigModelDescription;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonInclude(Include.NON_NULL)
public class DatabaseV4Response extends DatabaseV4Base {

    @Schema(description = ModelDescriptions.ID)
    private Long id;

    @Schema(description = ModelDescriptions.CREATED)
    private Long creationDate;

    @Schema(description = RDSConfigModelDescription.CLUSTER_NAMES)
    private Set<String> clusterNames;

    @Schema(description = Database.DB_ENGINE, required = true)
    private String databaseEngine;

    @Schema(description = Database.CONNECTION_DRIVER_NAME, required = true)
    private String connectionDriver;

    @Schema(description = Database.DB_ENGINE_DISPLAYNAME, required = true)
    private String databaseEngineDisplayName;

    @Schema(description = Database.USERNAME)
    private SecretResponse connectionUserName;

    @Schema(description = Database.PASSWORD)
    private SecretResponse connectionPassword;

    @Schema(description = ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceV4Response workspace;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    public Set<String> getClusterNames() {
        return clusterNames;
    }

    public void setClusterNames(Set<String> clusterNames) {
        this.clusterNames = clusterNames;
    }

    public String getDatabaseEngine() {
        return databaseEngine;
    }

    public void setDatabaseEngine(String databaseEngine) {
        this.databaseEngine = databaseEngine;
    }

    public String getConnectionDriver() {
        return connectionDriver;
    }

    public void setConnectionDriver(String connectionDriver) {
        this.connectionDriver = connectionDriver;
    }

    public String getDatabaseEngineDisplayName() {
        return databaseEngineDisplayName;
    }

    public void setDatabaseEngineDisplayName(String databaseEngineDisplayName) {
        this.databaseEngineDisplayName = databaseEngineDisplayName;
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

    public WorkspaceResourceV4Response getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceV4Response workspace) {
        this.workspace = workspace;
    }
}
