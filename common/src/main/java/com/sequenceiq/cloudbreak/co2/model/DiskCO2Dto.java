package com.sequenceiq.cloudbreak.co2.model;

public class DiskCO2Dto {

    private String diskType;

    private int size;

    private int count;

    public DiskCO2Dto() {
    }

    public DiskCO2Dto(String diskType, int size, int count) {
        this.diskType = diskType;
        this.size = size;
        this.count = count;
    }

    public String getDiskType() {
        return diskType;
    }

    public void setDiskType(String diskType) {
        this.diskType = diskType;
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

    @Override
    public String toString() {
        return "DiskCO2Dto{" +
                "diskType=" + diskType +
                ", size=" + size +
                ", count=" + count +
                '}';
    }
}
