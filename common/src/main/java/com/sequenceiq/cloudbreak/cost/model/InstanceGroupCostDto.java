package com.sequenceiq.cloudbreak.cost.model;

import java.util.List;

public class InstanceGroupCostDto {

    private int count;

    private double pricePerInstance;

    private double clouderaPricePerInstance;

    private List<DiskCostDto> disksPerInstance;

    public double getTotalProviderPrice() {
        return pricePerInstance * count;
    }

    public double getTotalClouderaPrice() {
        return clouderaPricePerInstance * count;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
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
                "count=" + count +
                ", pricePerInstance=" + pricePerInstance +
                ", clouderaPricePerInstance=" + clouderaPricePerInstance +
                ", disksPerInstance=" + disksPerInstance +
                '}';
    }
}
