package com.sequenceiq.cloudbreak.service.secret.vault;

public class VaultSecret {

    private String enginePath;

    private String engineClass;

    private String path;

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
