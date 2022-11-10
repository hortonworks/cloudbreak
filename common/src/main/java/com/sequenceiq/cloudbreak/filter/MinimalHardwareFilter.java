package com.sequenceiq.cloudbreak.filter;

import org.springframework.stereotype.Service;

@Service
public class MinimalHardwareFilter {

    private static final int MIN_CPU = 4;

    private static final int MIN_MEMORY_IN_GB = 16;

    public boolean suitableAsMinimumHardware(Integer cpu, Float memoryInGb) {
        return suitableAsMinimumHardwareForCpu(cpu) && suitableAsMinimumHardwareForMemory(memoryInGb);
    }

    public boolean suitableAsMinimumHardwareForCpu(Integer cpu) {
        boolean suitable = false;
        if (cpu != null) {
            suitable = cpu >= MIN_CPU;
        }
        return suitable;
    }

    public int minCpu() {
        return MIN_CPU;
    }

    public boolean suitableAsMinimumHardwareForMemory(Float memoryInGb) {
        boolean suitable = false;
        if (memoryInGb != null) {
            suitable = memoryInGb >= MIN_MEMORY_IN_GB;
        }
        return suitable;
    }

    public int minMemory() {
        return MIN_MEMORY_IN_GB;
    }

}
