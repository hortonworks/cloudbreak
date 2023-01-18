package com.sequenceiq.redbeams.api.endpoint.v4.database.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.database.base.DatabaseV4Base;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.Database;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.DATABASE_RESPONSE)
@JsonInclude(Include.NON_NULL)
public class DatabaseV4Response extends DatabaseV4Base {

    @Schema(description = Database.CRN)
    private String crn;

    @Schema(description = Database.CREATION_DATE)
    private Long creationDate;

    @Schema(description = Database.DB_ENGINE, required = true)
    private String databaseEngine;

    @Schema(description = Database.CONNECTION_DRIVER, required = true)
    private String connectionDriver;

    @Schema(description = Database.DB_ENGINE_DISPLAYNAME, required = true)
    private String databaseEngineDisplayName;

    @Schema(description = Database.USERNAME)
    private SecretResponse connectionUserName;

    @Schema(description = Database.PASSWORD)
    private SecretResponse connectionPassword;

    @Schema(description = Database.RESOURCE_STATUS)
    private ResourceStatus resourceStatus;

    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
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

    public ResourceStatus getResourceStatus() {
        return resourceStatus;
    }

    public void setResourceStatus(ResourceStatus resourceStatus) {
        this.resourceStatus = resourceStatus;
    }
}
