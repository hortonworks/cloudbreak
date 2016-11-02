package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

public class VmTypeMeta {

    public static final String CPU = "Cpu";

    public static final String MEMORY = "Memory";

    public static final String MAXIMUM_PERSISTENT_DISKS_SIZE_GB = "maximumPersistentDisksSizeGb";

    private VolumeParameterConfig magneticConfig;

    private VolumeParameterConfig autoAttachedConfig;

    private VolumeParameterConfig ssdConfig;

    private VolumeParameterConfig ephemeralConfig;

    private VolumeParameterConfig st1Config;

    private Map<String, String> properties = new HashMap<>();

    public VmTypeMeta() {
    }

    public VolumeParameterConfig getMagneticConfig() {
        return magneticConfig;
    }

    public void setMagneticConfig(VolumeParameterConfig magneticConfig) {
        this.magneticConfig = magneticConfig;
    }

    public VolumeParameterConfig getAutoAttachedConfig() {
        return autoAttachedConfig;
    }

    public void setAutoAttachedConfig(VolumeParameterConfig autoAttachedConfig) {
        this.autoAttachedConfig = autoAttachedConfig;
    }

    public VolumeParameterConfig getSsdConfig() {
        return ssdConfig;
    }

    public void setSsdConfig(VolumeParameterConfig ssdConfig) {
        this.ssdConfig = ssdConfig;
    }

    public VolumeParameterConfig getEphemeralConfig() {
        return ephemeralConfig;
    }

    public void setEphemeralConfig(VolumeParameterConfig ephemeralConfig) {
        this.ephemeralConfig = ephemeralConfig;
    }

    public VolumeParameterConfig getSt1Config() {
        return st1Config;
    }

    public void setSt1Config(VolumeParameterConfig st1Config) {
        this.st1Config = st1Config;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public static class VmTypeMetaBuilder {

        private VolumeParameterConfig magneticConfig;

        private VolumeParameterConfig autoAttachedConfig;

        private VolumeParameterConfig ssdConfig;

        private VolumeParameterConfig ephemeralConfig;

        private VolumeParameterConfig st1Config;

        private Map<String, String> properties = new HashMap<>();

        private VmTypeMetaBuilder() {
        }

        public static VmTypeMetaBuilder builder() {
            return new VmTypeMetaBuilder();
        }

        public VmTypeMetaBuilder withMagneticConfig(Integer minimumSize, Integer maximumSize, Integer minimumNumber, Integer maximumNumber) {
            this.magneticConfig = new VolumeParameterConfig(VolumeParameterType.MAGNETIC, minimumSize, maximumSize, minimumNumber, maximumNumber);
            return this;
        }

        public VmTypeMetaBuilder withMagneticConfig(VolumeParameterConfig volumeParameterConfig) {
            this.magneticConfig = volumeParameterConfig;
            return this;
        }

        public VmTypeMetaBuilder withAutoAttachedConfig(Integer minimumSize, Integer maximumSize, Integer minimumNumber, Integer maximumNumber) {
            this.autoAttachedConfig = new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, minimumSize, maximumSize, minimumNumber, maximumNumber);
            return this;
        }

        public VmTypeMetaBuilder withAutoAttachedConfig(VolumeParameterConfig volumeParameterConfig) {
            this.autoAttachedConfig = volumeParameterConfig;
            return this;
        }

        public VmTypeMetaBuilder withSsdConfig(Integer minimumSize, Integer maximumSize, Integer minimumNumber, Integer maximumNumber) {
            this.ssdConfig = new VolumeParameterConfig(VolumeParameterType.SSD, minimumSize, maximumSize, minimumNumber, maximumNumber);
            return this;
        }

        public VmTypeMetaBuilder withSsdConfig(VolumeParameterConfig volumeParameterConfig) {
            this.ssdConfig = volumeParameterConfig;
            return this;
        }

        public VmTypeMetaBuilder withEphemeralConfig(Integer minimumSize, Integer maximumSize, Integer minimumNumber, Integer maximumNumber) {
            this.ephemeralConfig = new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, minimumSize, maximumSize, minimumNumber, maximumNumber);
            return this;
        }

        public VmTypeMetaBuilder withEphemeralConfig(VolumeParameterConfig volumeParameterConfig) {
            this.ephemeralConfig = volumeParameterConfig;
            return this;
        }

        public VmTypeMetaBuilder withSt1Config(Integer minimumSize, Integer maximumSize, Integer minimumNumber, Integer maximumNumber) {
            this.st1Config = new VolumeParameterConfig(VolumeParameterType.ST1, minimumSize, maximumSize, minimumNumber, maximumNumber);
            return this;
        }

        public VmTypeMetaBuilder withSt1Config(VolumeParameterConfig volumeParameterConfig) {
            this.st1Config = volumeParameterConfig;
            return this;
        }

        public VmTypeMetaBuilder withProperty(String name, String value) {
            this.properties.put(name, value);
            return this;
        }

        public VmTypeMetaBuilder withCpuAndMemory(Integer cpu, Float memory) {
            this.properties.put(CPU, cpu.toString());
            this.properties.put(MEMORY, memory.toString());
            return this;
        }

        public VmTypeMetaBuilder withCpuAndMemory(String cpu, String memory) {
            this.properties.put(CPU, cpu);
            this.properties.put(MEMORY, memory);
            return this;
        }

        public VmTypeMetaBuilder withMaximumPersistentDisksSizeGb(Float maximumPersistentDisksSizeGb) {
            this.properties.put(MAXIMUM_PERSISTENT_DISKS_SIZE_GB, maximumPersistentDisksSizeGb.toString());
            return this;
        }

        public VmTypeMetaBuilder withMaximumPersistentDisksSizeGb(String maximumPersistentDisksSizeGb) {
            this.properties.put(MAXIMUM_PERSISTENT_DISKS_SIZE_GB, maximumPersistentDisksSizeGb);
            return this;
        }

        public VmTypeMeta create() {
            VmTypeMeta vmTypeMeta = new VmTypeMeta();
            vmTypeMeta.setAutoAttachedConfig(this.autoAttachedConfig);
            vmTypeMeta.setEphemeralConfig(this.ephemeralConfig);
            vmTypeMeta.setMagneticConfig(this.magneticConfig);
            vmTypeMeta.setSsdConfig(this.ssdConfig);
            vmTypeMeta.setSt1Config(this.st1Config);
            vmTypeMeta.setProperties(this.properties);
            return vmTypeMeta;
        }
    }
}
