package com.sequenceiq.cloudbreak.service.cost.model;

import java.util.List;

public class InstanceGroupCostDto {

    private String type;

    private int count;

    private int coresPerInstance;

    private int memoryPerInstance;

    private double pricePerInstance;

    private List<DiskCostDto> disksPerInstance;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCoresPerInstance() {
        return coresPerInstance;
    }

    public void setCoresPerInstance(int coresPerInstance) {
        this.coresPerInstance = coresPerInstance;
    }

    public int getMemoryPerInstance() {
        return memoryPerInstance;
    }

    public void setMemoryPerInstance(int memoryPerInstance) {
        this.memoryPerInstance = memoryPerInstance;
    }

    public List<DiskCostDto> getDisksPerInstance() {
        return disksPerInstance;
    }

    public void setDisksPerInstance(List<DiskCostDto> disksPerInstance) {
        this.disksPerInstance = disksPerInstance;
    }
}
