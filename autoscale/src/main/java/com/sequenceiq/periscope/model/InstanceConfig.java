package com.sequenceiq.periscope.model;

public class InstanceConfig {

    private String instanceName;

    private Integer coreCPU;

    private Long memoryInMb;

    private Boolean defaultValueUsed = false;

    public InstanceConfig(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public Integer getCoreCPU() {
        return coreCPU;
    }

    public void setCoreCPU(Integer coreCPU) {
        this.coreCPU = coreCPU;
    }

    public Long getMemoryInMb() {
        return memoryInMb;
    }

    public void setMemoryInMb(Long memoryInMb) {
        this.memoryInMb = memoryInMb;
    }

    public void setDefaultValueUsed(Boolean toBeCached) {
        defaultValueUsed = toBeCached;
    }

    public Boolean getDefaultValueUsed() {
        return defaultValueUsed;
    }
}
