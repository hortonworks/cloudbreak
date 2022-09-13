package com.sequenceiq.cloudbreak.cost.model;

import java.util.List;

public class InstanceGroupCostDto {

    private String type;

    private int count;

    private int coresPerInstance;

    private int memoryPerInstance;

    private double pricePerInstance;

    private double clouderaPricePerInstance;

    private List<DiskCostDto> disksPerInstance;

    public int getTotalvCpuCores() {
        return coresPerInstance * count;
    }

    public int getTotalMemoryInGb() {
        return memoryPerInstance * count;
    }

    public double getTotalProviderPrice() {
        return pricePerInstance * count;
    }

    public double getTotalClouderaPrice() {
        return clouderaPricePerInstance * count;
    }

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

    public double getPricePerInstance() {
        return pricePerInstance;
    }

    public void setPricePerInstance(double pricePerInstance) {
        this.pricePerInstance = pricePerInstance;
    }

    public double getClouderaPricePerInstance() {
        return clouderaPricePerInstance;
    }

    public void setClouderaPricePerInstance(double clouderaPricePerInstance) {
        this.clouderaPricePerInstance = clouderaPricePerInstance;
    }

    @Override
    public String toString() {
        return "InstanceGroupCostDto{" +
                "type='" + type + '\'' +
                ", count=" + count +
                ", coresPerInstance=" + coresPerInstance +
                ", memoryPerInstance=" + memoryPerInstance +
                ", pricePerInstance=" + pricePerInstance +
                ", clouderaPricePerInstance=" + clouderaPricePerInstance +
                ", disksPerInstance=" + disksPerInstance +
                '}';
    }
}
