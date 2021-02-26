package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

public class VmTypeMeta {

    public static final String CPU = "Cpu";

    public static final String MEMORY = "Memory";

    public static final String MAXIMUM_PERSISTENT_DISKS_SIZE_GB = "maximumPersistentDisksSizeGb";

    public static final String PRICE = "Price";

    public static final String VOLUME_ENCRYPTION_SUPPORTED = "EncryptionSupported";

    private VolumeParameterConfig magneticConfig;

    private VolumeParameterConfig autoAttachedConfig;

    private VolumeParameterConfig ssdConfig;

    private VolumeParameterConfig ephemeralConfig;

    private VolumeParameterConfig st1Config;

    private Map<String, Object> properties = new HashMap<>();

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

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Integer getCPU() {
        Object cpuAsObject = properties.get(CPU);
        return cpuAsObject != null ? Integer.valueOf(cpuAsObject.toString()) : null;
    }

    public Float getMemoryInGb() {
        Object memoryAsObject = properties.get(MEMORY);
        return memoryAsObject != null ? Float.valueOf(memoryAsObject.toString()) : null;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "VmTypeMeta{"
                + "magneticConfig=" + magneticConfig
                + ", autoAttachedConfig=" + autoAttachedConfig
                + ", ssdConfig=" + ssdConfig
                + ", ephemeralConfig=" + ephemeralConfig
                + ", st1Config=" + st1Config
                + ", properties=" + properties
                + '}';
    }

    public static class VmTypeMetaBuilder {

        private VolumeParameterConfig magneticConfig;

        private VolumeParameterConfig autoAttachedConfig;

        private VolumeParameterConfig ssdConfig;

        private VolumeParameterConfig ephemeralConfig;

        private VolumeParameterConfig st1Config;

        private final Map<String, Object> properties = new HashMap<>();

        private VmTypeMetaBuilder() {
        }

        public static VmTypeMetaBuilder builder() {
            return new VmTypeMetaBuilder();
        }

        public VmTypeMetaBuilder withMagneticConfig(Integer minimumSize, Integer maximumSize, Integer minimumNumber, Integer maximumNumber) {
            magneticConfig = new VolumeParameterConfig(VolumeParameterType.MAGNETIC, minimumSize, maximumSize, minimumNumber, maximumNumber);
            return this;
        }

        public VmTypeMetaBuilder withMagneticConfig(VolumeParameterConfig volumeParameterConfig) {
            magneticConfig = volumeParameterConfig;
            return this;
        }

        public VmTypeMetaBuilder withAutoAttachedConfig(Integer minimumSize, Integer maximumSize, Integer minimumNumber, Integer maximumNumber) {
            autoAttachedConfig = new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, minimumSize, maximumSize, minimumNumber, maximumNumber);
            return this;
        }

        public VmTypeMetaBuilder withAutoAttachedConfig(VolumeParameterConfig volumeParameterConfig) {
            autoAttachedConfig = volumeParameterConfig;
            return this;
        }

        public VmTypeMetaBuilder withSsdConfig(Integer minimumSize, Integer maximumSize, Integer minimumNumber, Integer maximumNumber) {
            ssdConfig = new VolumeParameterConfig(VolumeParameterType.SSD, minimumSize, maximumSize, minimumNumber, maximumNumber);
            return this;
        }

        public VmTypeMetaBuilder withSsdConfig(VolumeParameterConfig volumeParameterConfig) {
            ssdConfig = volumeParameterConfig;
            return this;
        }

        public VmTypeMetaBuilder withEphemeralConfig(Integer minimumSize, Integer maximumSize, Integer minimumNumber, Integer maximumNumber) {
            ephemeralConfig = new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, minimumSize, maximumSize, minimumNumber, maximumNumber);
            return this;
        }

        public VmTypeMetaBuilder withEphemeralConfig(VolumeParameterConfig volumeParameterConfig) {
            ephemeralConfig = volumeParameterConfig;
            return this;
        }

        public VmTypeMetaBuilder withSt1Config(Integer minimumSize, Integer maximumSize, Integer minimumNumber, Integer maximumNumber) {
            st1Config = new VolumeParameterConfig(VolumeParameterType.ST1, minimumSize, maximumSize, minimumNumber, maximumNumber);
            return this;
        }

        public VmTypeMetaBuilder withSt1Config(VolumeParameterConfig volumeParameterConfig) {
            st1Config = volumeParameterConfig;
            return this;
        }

        public VmTypeMetaBuilder withProperty(String name, String value) {
            properties.put(name, value);
            return this;
        }

        public VmTypeMetaBuilder withCpuAndMemory(Integer cpu, Float memory) {
            properties.put(CPU, cpu);
            properties.put(MEMORY, memory);
            return this;
        }

        public VmTypeMetaBuilder withCpuAndMemory(int cpu, int memory) {
            properties.put(CPU, cpu);
            properties.put(MEMORY, memory);
            return this;
        }

        public VmTypeMetaBuilder withMaximumPersistentDisksSizeGb(Float maximumPersistentDisksSizeGb) {
            properties.put(MAXIMUM_PERSISTENT_DISKS_SIZE_GB, maximumPersistentDisksSizeGb);
            return this;
        }

        public VmTypeMetaBuilder withMaximumPersistentDisksSizeGb(Long maximumPersistentDisksSizeGb) {
            properties.put(MAXIMUM_PERSISTENT_DISKS_SIZE_GB, maximumPersistentDisksSizeGb);
            return this;
        }

        public VmTypeMetaBuilder withPrice(Double price) {
            properties.put(PRICE, price.toString());
            return this;
        }

        public VmTypeMetaBuilder withVolumeEncryptionSupport(boolean supportEncryption) {
            properties.put(VOLUME_ENCRYPTION_SUPPORTED, supportEncryption);
            return this;
        }

        public VmTypeMeta create() {
            VmTypeMeta vmTypeMeta = new VmTypeMeta();
            vmTypeMeta.setAutoAttachedConfig(autoAttachedConfig);
            vmTypeMeta.setEphemeralConfig(ephemeralConfig);
            vmTypeMeta.setMagneticConfig(magneticConfig);
            vmTypeMeta.setSsdConfig(ssdConfig);
            vmTypeMeta.setSt1Config(st1Config);
            vmTypeMeta.setProperties(properties);
            return vmTypeMeta;
        }

    }
}
