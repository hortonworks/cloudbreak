package com.sequenceiq.redbeams.api.endpoint.v4.database.base;

import static com.sequenceiq.redbeams.doc.ModelDescriptions.DESCRIPTION;
import static com.sequenceiq.redbeams.doc.ModelDescriptions.Database;
import static com.sequenceiq.redbeams.doc.ModelDescriptions.ENVIRONMENT_ID;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModelProperty;

// TODO add validation
//@ValidRDSConfigJson
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class DatabaseV4Base implements Serializable {

    @NotNull
    @Size(max = 100, min = 5, message = "The length of the database config's name must be between 5 to 100")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The database's name can only contain lowercase alphanumeric characters, and hyphens, and must start with an alphanumeric character")
    @ApiModelProperty(value = Database.NAME, required = true)
    private String name;

    @Size(max = 1000)
    @ApiModelProperty(DESCRIPTION)
    private String description;

    @NotNull
    @ApiModelProperty(value = Database.CONNECTION_URL, required = true)
    private String connectionURL;

    @NotNull
    @ApiModelProperty(value = Database.TYPE, required = true)
    private String type;

    @ApiModelProperty(Database.CONNECTOR_JAR_URL)
    private String connectorJarUrl;

    @ApiModelProperty(ENVIRONMENT_ID)
    private String environmentId;

    public String getConnectionURL() {
        return connectionURL;
    }

    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConnectorJarUrl() {
        return connectorJarUrl;
    }

    public void setConnectorJarUrl(String connectorJarUrl) {
        this.connectorJarUrl = connectorJarUrl;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }
}
