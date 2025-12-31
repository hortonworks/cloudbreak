package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ConnectorModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseVirtualMachinesV4Response implements JsonEntity {

    @Schema(description = ConnectorModelDescription.VIRTUAL_MACHNES)
    private Set<DatabaseVmTypeV4Response> databaseVirtualMachines = new HashSet<>();

    @Schema(description = ConnectorModelDescription.DEFAULT_VIRTUAL_MACHINES)
    private DatabaseVmTypeV4Response defaultDatabaseVirtualMachine;

    public Set<DatabaseVmTypeV4Response> getDatabaseVirtualMachines() {
        return databaseVirtualMachines;
    }

    public void setDatabaseVirtualMachines(Set<DatabaseVmTypeV4Response> databaseVirtualMachines) {
        this.databaseVirtualMachines = databaseVirtualMachines;
    }

    public DatabaseVmTypeV4Response getDefaultDatabaseVirtualMachine() {
        return defaultDatabaseVirtualMachine;
    }

    public void setDefaultDatabaseVirtualMachine(DatabaseVmTypeV4Response defaultDatabaseVirtualMachine) {
        this.defaultDatabaseVirtualMachine = defaultDatabaseVirtualMachine;
    }
}
