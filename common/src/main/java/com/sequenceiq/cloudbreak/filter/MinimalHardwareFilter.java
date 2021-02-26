package com.sequenceiq.cloudbreak.filter;

import org.springframework.stereotype.Service;

@Service
public class MinimalHardwareFilter {

    private static final int MIN_CPU = 4;

    private static final int MIN_MEMORY_IN_GB = 16;

    public boolean suitableAsMinimumHardware(Integer cpu, Float memoryInGb) {
        boolean suitable = false;
        if (cpu != null && memoryInGb != null) {
            suitable = cpu >= MIN_CPU && memoryInGb >= MIN_MEMORY_IN_GB;
        }
        return suitable;
    }

}
