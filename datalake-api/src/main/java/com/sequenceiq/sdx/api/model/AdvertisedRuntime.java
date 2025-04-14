package com.sequenceiq.sdx.api.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.model.Architecture;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdvertisedRuntime {

    @Schema(description = ModelDescriptions.RUNTIME_VERSION)
    private String runtimeVersion;

    @Schema(description = ModelDescriptions.DEFAULT_RUNTIME_VERSION, requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean defaultRuntimeVersion;

    @Schema(description = ModelDescriptions.ARCHITECTURE)
    private Architecture architecture;

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

    public Architecture getArchitecture() {
        return architecture;
    }

    public void setArchitecture(Architecture architecture) {
        this.architecture = architecture;
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
        return defaultRuntimeVersion == that.defaultRuntimeVersion && Objects.equals(runtimeVersion, that.runtimeVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runtimeVersion, defaultRuntimeVersion, architecture);
    }

    @Override
    public String toString() {
        return "AdvertisedRuntime{" + "runtimeVersion='" + runtimeVersion + '\'' +
                ", defaultRuntimeVersion=" + defaultRuntimeVersion + ", architecture=" + architecture + '}';
    }
}
