package com.sequenceiq.cloudbreak.cloud.model;

import static com.sequenceiq.common.model.Architecture.ARM64;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.model.Architecture;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultVmTypes {

    @JsonProperty("database")
    private ArchitectureVmTypes database;

    @JsonProperty("freeipa")
    private ArchitectureVmTypes freeipa;

    public DefaultVmTypes() {
    }

    public DefaultVmTypes(ArchitectureVmTypes database, ArchitectureVmTypes freeipa) {
        this.database = database;
        this.freeipa = freeipa;
    }

    public ArchitectureVmTypes getDatabase() {
        return database;
    }

    public void setDatabase(ArchitectureVmTypes database) {
        this.database = database;
    }

    public ArchitectureVmTypes getFreeipa() {
        return freeipa;
    }

    public void setFreeipa(ArchitectureVmTypes freeipa) {
        this.freeipa = freeipa;
    }

    public List<String> getDatabaseVmType(Architecture architecture) {
        if (database == null) {
            return null;
        }
        if (ARM64.equals(architecture)) {
            return database.get(ARM64);
        }
        return database.get(Architecture.X86_64);
    }

    public List<String> getFreeipaVmType(Architecture architecture) {
        if (freeipa == null) {
            return null;
        }
        if (ARM64.equals(architecture)) {
            return freeipa.get(ARM64);
        }
        return freeipa.get(Architecture.X86_64);
    }
}
