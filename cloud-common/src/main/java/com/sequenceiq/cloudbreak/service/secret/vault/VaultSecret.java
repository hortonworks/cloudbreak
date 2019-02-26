package com.sequenceiq.cloudbreak.service.secret.vault;

public class VaultSecret {

    private final String enginePath;

    private final String engineClass;

    private final String path;

    public VaultSecret(String enginePath, String engineClass, String path) {
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
