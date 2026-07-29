package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.common.model.Architecture;

public class DatabaseVmTypeMeta {

    public static final String CPU = "Cpu";

    public static final String MEMORY = "Memory";

    public static final String PRICE = "Price";

    public static final String ARCHITECTURE = "Architecture";

    private List<String> availabilityZones = new ArrayList<>();

    private Map<String, Object> properties = new HashMap<>();

    public List<String> getAvailabilityZones() {
        return availabilityZones;
    }

    public void setAvailabilityZones(List<String> availabilityZones) {
        this.availabilityZones = availabilityZones;
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

    public Architecture getArchitecture() {
        return Architecture.fromStringWithFallback(properties.get(ARCHITECTURE));
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "DatabaseVmTypeMeta{" +
                "availabilityZones=" + availabilityZones +
                ", properties=" + properties +
                ", cpu=" + getCPU() +
                ", memoryInGb=" + getMemoryInGb() +
                ", architecture=" + getArchitecture() +
                '}';
    }

    public static class DatabaseVmTypeMetaBuilder {

        private List<String> availabilityZones;

        private final Map<String, Object> properties = new HashMap<>();

        private DatabaseVmTypeMetaBuilder() {
        }

        public static DatabaseVmTypeMetaBuilder builder() {
            return new DatabaseVmTypeMetaBuilder();
        }

        public DatabaseVmTypeMetaBuilder withAvailabilityZones(List<String> azs) {
            availabilityZones = azs;
            return this;
        }

        public DatabaseVmTypeMetaBuilder withProperty(String name, String value) {
            properties.put(name, value);
            return this;
        }

        public DatabaseVmTypeMetaBuilder withCpuAndMemory(Integer cpu, Float memory) {
            properties.put(CPU, cpu);
            properties.put(MEMORY, memory);
            return this;
        }

        public DatabaseVmTypeMetaBuilder withMemory(Float memory) {
            properties.put(MEMORY, memory);
            return this;
        }

        public DatabaseVmTypeMetaBuilder withPrice(Double price) {
            properties.put(PRICE, price.toString());
            return this;
        }

        public DatabaseVmTypeMetaBuilder withArchitecture(Architecture architecture) {
            properties.put(ARCHITECTURE, architecture);
            return this;
        }

        public DatabaseVmTypeMeta create() {
            DatabaseVmTypeMeta vmTypeMeta = new DatabaseVmTypeMeta();
            vmTypeMeta.setAvailabilityZones(availabilityZones);
            vmTypeMeta.setProperties(properties);
            return vmTypeMeta;
        }

    }
}
