package com.sequenceiq.redbeams.api.endpoint.v4.database.request;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.Database;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A request for creating a database on a database server.
 */
@ApiModel(description = ModelDescriptions.CREATE_DATABASE_REQUEST)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateDatabaseV4Request implements Serializable {

    @NotNull
    @ApiModelProperty(DatabaseServer.CRN)
    private String existingDatabaseServerCrn;

    @NotNull
    @ApiModelProperty(Database.NAME)
    private String databaseName;

    @NotNull
    @ApiModelProperty(Database.TYPE)
    private String type;

    /**
     * Gets the crn of the existing database on which to create the schema.
     *
     * @return the existing database name
     */
    public String getExistingDatabaseServerCrn() {
        return existingDatabaseServerCrn;
    }

    /**
     * Sets the crn of the existing database on which to create the schema.
     *
     * @param existingDatabaseServerCrn the existing database name
     */
    public void setExistingDatabaseServerCrn(String existingDatabaseServerCrn) {
        this.existingDatabaseServerCrn = existingDatabaseServerCrn;
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
