package com.sequenceiq.cloudbreak.cost.model;

public class DiskCostDto {

    private int size;

    private int count;

    private double pricePerDiskGB;

    public DiskCostDto() {
    }

    public DiskCostDto(int count, int size, double pricePerDiskGB) {
        this.count = count;
        this.size = size;
        this.pricePerDiskGB = pricePerDiskGB;
    }

    public double getTotalDiskPrice() {
        return getTotalDiskSizeInGb() * pricePerDiskGB;
    }

    public int getTotalDiskSizeInGb() {
        return size * count;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getPricePerDiskGB() {
        return pricePerDiskGB;
    }

    public void setPricePerDiskGB(double pricePerDiskGB) {
        this.pricePerDiskGB = pricePerDiskGB;
    }
}
