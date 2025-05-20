package com.sequenceiq.cloudbreak.service.database;

public class DbOverrideVersion {
    private String minRuntimeVersion;

    private String engineVersion;

    public String getMinRuntimeVersion() {
        return minRuntimeVersion;
    }

    public void setMinRuntimeVersion(String minRuntimeVersion) {
        this.minRuntimeVersion = minRuntimeVersion;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    @Override
    public String toString() {
        return "DbOverrideVersion{" +
                "minRuntimeVersion='" + minRuntimeVersion + '\'' +
                ", engineVersion='" + engineVersion + '\'' +
                '}';
    }
}
