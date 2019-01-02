package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.HashMap;
import java.util.Map;

public class VmTypeMetaV4Response {

    private VolumeParameterConfigV4Response magneticConfig;

    private VolumeParameterConfigV4Response autoAttachedConfig;

    private VolumeParameterConfigV4Response ssdConfig;

    private VolumeParameterConfigV4Response ephemeralConfig;

    private VolumeParameterConfigV4Response st1Config;

    private Map<String, String> properties = new HashMap<>();

    public VolumeParameterConfigV4Response getMagneticConfig() {
        return magneticConfig;
    }

    public void setMagneticConfig(VolumeParameterConfigV4Response magneticConfig) {
        this.magneticConfig = magneticConfig;
    }

    public VolumeParameterConfigV4Response getAutoAttachedConfig() {
        return autoAttachedConfig;
    }

    public void setAutoAttachedConfig(VolumeParameterConfigV4Response autoAttachedConfig) {
        this.autoAttachedConfig = autoAttachedConfig;
    }

    public VolumeParameterConfigV4Response getSsdConfig() {
        return ssdConfig;
    }

    public void setSsdConfig(VolumeParameterConfigV4Response ssdConfig) {
        this.ssdConfig = ssdConfig;
    }

    public VolumeParameterConfigV4Response getEphemeralConfig() {
        return ephemeralConfig;
    }

    public void setEphemeralConfig(VolumeParameterConfigV4Response ephemeralConfig) {
        this.ephemeralConfig = ephemeralConfig;
    }

    public VolumeParameterConfigV4Response getSt1Config() {
        return st1Config;
    }

    public void setSt1Config(VolumeParameterConfigV4Response st1Config) {
        this.st1Config = st1Config;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

}
