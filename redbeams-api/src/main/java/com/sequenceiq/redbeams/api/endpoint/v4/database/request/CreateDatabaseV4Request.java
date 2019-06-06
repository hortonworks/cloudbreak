package com.sequenceiq.redbeams.api.endpoint.v4.database.request;

import static com.sequenceiq.redbeams.doc.ModelDescriptions.ENVIRONMENT_ID;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.redbeams.doc.ModelDescriptions.Database;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A request for creating a database on a database server.
 */
@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateDatabaseV4Request implements Serializable {

    @ApiModelProperty(ENVIRONMENT_ID)
    private String environmentId;

    @ApiModelProperty(DatabaseServer.NAME)
    private String existingDatabaseServerName;

    @ApiModelProperty(Database.NAME)
    private String databaseName;

    @ApiModelProperty(Database.TYPE)
    private String type;

    /**
     * Gets the environment ID associated with the existing database server. This will also be used
     * as the environment for the created database.
     *
     * @return the environment ID
     */
    public String getEnvironmentId() {
        return environmentId;
    }

    /**
     * Gets the environment ID associated with the existing database server. This will also be used
     * as the environment for the created database.
     *
     * @param environmentId the environment ID
     */
    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    /**
     * Gets the name of the existing database on which to create the schema.
     *
     * @return the existing database name
     */
    public String getExistingDatabaseServerName() {
        return existingDatabaseServerName;
    }

    /**
     * Sets the name of the existing database on which to create the schema.
     *
     * @param existingDatabaseServerName the existing database name
     */
    public void setExistingDatabaseServerName(String existingDatabaseServerName) {
        this.existingDatabaseServerName = existingDatabaseServerName;
    }

    /**
     * Gets the database name to create.
     *
     * @return the database name
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Sets the database name to create.
     *
     * @param databaseName the database name
     */
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * Gets the database type.
     *
     * @return the database type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the database type.
     *
     * @param type the database type
     */
    public void setType(String type) {
        this.type = type;
    }
}
