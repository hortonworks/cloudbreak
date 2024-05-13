package com.sequenceiq.cloudbreak.service.secret.vault;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultSecret {

    private final String enginePath;

    private final String engineClass;

    private final String path;

    private final Integer version;

    @JsonCreator
    public VaultSecret(@JsonProperty("enginePath") String enginePath,
            @JsonProperty("engineClass") String engineClass,
            @JsonProperty("path") String path,
            @JsonProperty("version") Integer version) {
        this.enginePath = enginePath;
        this.engineClass = engineClass;
        this.path = path;
        this.version = version;
    }

    public String getEnginePath() {
        return enginePath;
    }

    public String getEngineClass() {
        return engineClass;
    }

    public String getPath() {
        return path;
    }

    public Integer getVersion() {
        return version;
    }
}
