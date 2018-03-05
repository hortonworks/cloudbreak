package com.sequenceiq.cloudbreak.api.model.rds;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RDSConfig;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class RDSConfigJson implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = RDSConfig.NAME, required = true)
    @Size(max = 50, min = 4, message = "The length of the name has to be in range of 4 to 50")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    private String name;

    @NotNull
    @Pattern(regexp = "^jdbc:postgresql://[-\\w\\.]*:\\d{1,5}/?\\w*", message = "Connection URL is not valid")
    @ApiModelProperty(value = RDSConfig.CONNECTION_URL, required = true)
    private String connectionURL;

    @NotNull
    @ApiModelProperty(value = RDSConfig.DB_ENGINE, required = true)
    private String databaseEngine;

    @NotNull
    @ApiModelProperty(value = RDSConfig.CONNECTION_DRIVER_NAME, required = true)
    private String connectionDriver = "org.postgresql.Driver";

    @ApiModelProperty(RDSConfig.VALIDATED)
    private boolean validated = true;

    @NotNull
    @ApiModelProperty(value = RDSConfig.RDSTYPE, required = true)
    @Size(max = 12, min = 4, message = "The length of the type has to be in range of 4 to 12")
    @Pattern(regexp = "(^[a-zA-Z][-a-zA-Z0-9]*[a-zA-Z0-9]$)",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    private String type = RdsType.HIVE.name();

    public String getConnectionURL() {
        return connectionURL;
    }

    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
