package com.sequenceiq.cloudbreak.service.secret.vault;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultSecret {

    private String enginePath;

    private String engineClass;

    private String path;

    @JsonCreator
    public VaultSecret(@JsonProperty("enginePath") String enginePath,
            @JsonProperty("engineClass") String engineClass,
            @JsonProperty("path") String path) {
        this.enginePath = enginePath;
        this.engineClass = engineClass;
        this.path = path;
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
}
