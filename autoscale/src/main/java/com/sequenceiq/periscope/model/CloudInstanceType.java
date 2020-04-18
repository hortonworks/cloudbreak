package com.sequenceiq.periscope.model;

public class CloudInstanceType {

    private static final Integer GB_TO_MB = 1024;

    private String instanceName;

    private Integer coreCPU;

    private Float memoryInGB;

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public Integer getCoreCPU() {
        return coreCPU;
    }

    public void setCoreCPU(Integer coreCPU) {
        this.coreCPU = coreCPU;
    }

    public Float getMemoryInGB() {
        return memoryInGB;
    }

    public void setMemoryInGB(Float memoryInGB) {
        this.memoryInGB = memoryInGB;
    }

    public Integer getMemoryInMB() {
        return Math.round(memoryInGB * GB_TO_MB);
    }
}
