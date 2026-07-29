package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DatabaseVmTypeResponse implements Serializable {

    @Schema
    private String value;

    @Schema
    private DatabaseVmTypeMetaJson databaseVmTypeMetaJson;

    public DatabaseVmTypeResponse() {
        this(null, null);
    }

    public DatabaseVmTypeResponse(String value) {
        this(value, null);
    }

    public DatabaseVmTypeResponse(String value, DatabaseVmTypeMetaJson databaseVmTypeMetaJson) {
        this.value = value;
        this.databaseVmTypeMetaJson = databaseVmTypeMetaJson;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public DatabaseVmTypeMetaJson getDatabaseVmTypeMetaJson() {
        return databaseVmTypeMetaJson;
    }

    public void setDatabaseVmTypeMetaJson(DatabaseVmTypeMetaJson databaseVmTypeMetaJson) {
        this.databaseVmTypeMetaJson = databaseVmTypeMetaJson;
    }

    @Override
    public String toString() {
        return "DatabaseVmTypeResponse{" +
                "value='" + value + '\'' +
                ", databaseVmTypeMetaJson=" + databaseVmTypeMetaJson +
                '}';
    }
}
