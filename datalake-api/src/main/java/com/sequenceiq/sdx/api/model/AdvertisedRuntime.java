package com.sequenceiq.sdx.api.model;

import java.util.Objects;

public class AdvertisedRuntime {

    private String runtimeVersion;

    private boolean defaultRuntimeVersion;

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }

    public boolean isDefaultRuntimeVersion() {
        return defaultRuntimeVersion;
    }

    public void setDefaultRuntimeVersion(boolean defaultRuntimeVersion) {
        this.defaultRuntimeVersion = defaultRuntimeVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AdvertisedRuntime that = (AdvertisedRuntime) o;
        return defaultRuntimeVersion == that.defaultRuntimeVersion &&
                Objects.equals(runtimeVersion, that.runtimeVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runtimeVersion, defaultRuntimeVersion);
    }

    @Override
    public String toString() {
        return "AdvertisedRuntime{" +
                "runtimeVersion='" + runtimeVersion + '\'' +
                ", defaultRuntimeVersion=" + defaultRuntimeVersion +
                '}';
    }
}
