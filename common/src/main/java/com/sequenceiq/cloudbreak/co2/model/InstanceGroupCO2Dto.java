package com.sequenceiq.cloudbreak.co2.model;

import java.util.List;

public class InstanceGroupCO2Dto {

    private int count;

    private int vCPUs;

    private int memory;

    private List<DiskCO2Dto> disksPerInstance;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getvCPUs() {
        return vCPUs;
    }

    public void setvCPUs(int vCPUs) {
        this.vCPUs = vCPUs;
    }

    public int getMemory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    public List<DiskCO2Dto> getDisksPerInstance() {
        return disksPerInstance;
    }

    public void setDisksPerInstance(List<DiskCO2Dto> disksPerInstance) {
        this.disksPerInstance = disksPerInstance;
    }

    @Override
    public String toString() {
        return "InstanceGroupCO2Dto{" +
                "count=" + count +
                ", vCPUCount=" + vCPUs +
                ", memory=" + memory +
                ", disksPerInstance=" + disksPerInstance +
                '}';
    }
}
