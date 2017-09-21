package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;

public class VmTypeMetaResponse {

    private VolumeParameterConfigJson magneticConfig;

    private VolumeParameterConfigJson autoAttachedConfig;

    private VolumeParameterConfigJson ssdConfig;

    private VolumeParameterConfigJson ephemeralConfig;

    private VolumeParameterConfigJson st1Config;

    private Map<String, String> properties = new HashMap<>();

    public VolumeParameterConfigJson getMagneticConfig() {
        return magneticConfig;
    }

    public void setMagneticConfig(VolumeParameterConfigJson magneticConfig) {
        this.magneticConfig = magneticConfig;
    }

    public VolumeParameterConfigJson getAutoAttachedConfig() {
        return autoAttachedConfig;
    }

    public void setAutoAttachedConfig(VolumeParameterConfigJson autoAttachedConfig) {
        this.autoAttachedConfig = autoAttachedConfig;
    }

    public VolumeParameterConfigJson getSsdConfig() {
        return ssdConfig;
    }

    public void setSsdConfig(VolumeParameterConfigJson ssdConfig) {
        this.ssdConfig = ssdConfig;
    }

    public VolumeParameterConfigJson getEphemeralConfig() {
        return ephemeralConfig;
    }

    public void setEphemeralConfig(VolumeParameterConfigJson ephemeralConfig) {
        this.ephemeralConfig = ephemeralConfig;
    }

    public VolumeParameterConfigJson getSt1Config() {
        return st1Config;
    }

    public void setSt1Config(VolumeParameterConfigJson st1Config) {
        this.st1Config = st1Config;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

}
