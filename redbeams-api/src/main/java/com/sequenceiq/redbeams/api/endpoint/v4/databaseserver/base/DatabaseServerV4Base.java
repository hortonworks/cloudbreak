package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.base;

import java.io.Serializable;
import java.util.StringJoiner;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.cloudbreak.validation.ValidDatabaseVendor;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class DatabaseServerV4Base implements Serializable {

    @NotNull
    @Size(max = 100, min = 5, message = "The length of the database server's name must be between 5 and 100, inclusive")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The database server's name may only contain lowercase letters, digits, and hyphens, and must start with an alphanumeric character")
    @Schema(description = DatabaseServer.NAME, required = true)
    private String name;

    @Size(max = 1000000)
    @Schema(description = DatabaseServer.DESCRIPTION)
    private String description;

    @NotNull
    @Schema(description = DatabaseServer.HOST, required = true)
    private String host;

    @NotNull
    @Schema(description = DatabaseServer.PORT, required = true)
    private Integer port;

    @NotNull
    @ValidDatabaseVendor
    @Schema(description = DatabaseServer.DATABASE_VENDOR, required = true)
    private String databaseVendor;

    @Schema(description = DatabaseServer.CONNECTION_DRIVER)
    private String connectionDriver;

    @NotNull
    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @Schema(description = DatabaseServer.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDatabaseVendor() {
        return databaseVendor;
    }

    public void setDatabaseVendor(String databaseVendor) {
        this.databaseVendor = databaseVendor;
    }

    public String getConnectionDriver() {
        return connectionDriver;
    }

    public void setConnectionDriver(String connectionDriver) {
        this.connectionDriver = connectionDriver;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DatabaseServerV4Base.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("description='" + description + "'")
                .add("host='" + host + "'")
                .add("port=" + port)
                .add("databaseVendor='" + databaseVendor + "'")
                .add("connectionDriver='" + connectionDriver + "'")
                .add("environmentCrn='" + environmentCrn + "'")
                .toString();
    }
}
