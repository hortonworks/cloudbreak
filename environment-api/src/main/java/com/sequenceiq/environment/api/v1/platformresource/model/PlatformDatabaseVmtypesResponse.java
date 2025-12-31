package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformDatabaseVmtypesResponse implements Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, DatabaseVirtualMachinesResponse> databaseVmTypes = new HashMap<>();

    public PlatformDatabaseVmtypesResponse() {
    }

    public PlatformDatabaseVmtypesResponse(Map<String, DatabaseVirtualMachinesResponse> databaseVmTypes) {
        this.databaseVmTypes = databaseVmTypes;
    }

    public Map<String, DatabaseVirtualMachinesResponse> getDatabaseVmTypes() {
        return databaseVmTypes;
    }

    public void setDatabaseVmTypes(Map<String, DatabaseVirtualMachinesResponse> databaseVmTypes) {
        this.databaseVmTypes = databaseVmTypes;
    }

    @Override
    public String toString() {
        return "PlatformDatabaseVmtypesResponse{" +
                "databaseVmTypes=" + databaseVmTypes +
                '}';
    }
}
