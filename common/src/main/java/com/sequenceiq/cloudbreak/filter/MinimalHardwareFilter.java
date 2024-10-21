package com.sequenceiq.cloudbreak.filter;

import org.springframework.stereotype.Service;

import com.sequenceiq.common.api.type.CdpResourceType;

@Service
public class MinimalHardwareFilter {

    private static final int MIN_CPU = 4;

    private static final int MIN_FREEIPA_CPU = 2;

    private static final int MIN_MEMORY_IN_GB = 16;

    private static final int MIN_FREEIPA_MEMORY_IN_GB = 8;

    public boolean suitableAsMinimumHardware(Integer cpu, Float memoryInGb) {
        return suitableAsMinimumHardwareForCpu(cpu, CdpResourceType.DEFAULT) && suitableAsMinimumHardwareForMemory(memoryInGb, CdpResourceType.DEFAULT);
    }

    public boolean suitableAsMinimumHardware(Integer cpu, Float memoryInGb, CdpResourceType cdpResourceType) {
        return suitableAsMinimumHardwareForCpu(cpu, cdpResourceType) && suitableAsMinimumHardwareForMemory(memoryInGb, cdpResourceType);
    }

    public boolean suitableAsMinimumHardwareForCpu(Integer cpu, CdpResourceType cdpResourceType) {
        boolean suitable = false;
        if (cpu != null) {
            if (CdpResourceType.FREEIPA.equals(cdpResourceType)) {
                suitable = cpu >= MIN_FREEIPA_CPU;
            } else {
                suitable = cpu >= MIN_CPU;
            }
        }
        return suitable;
    }

    public int minCpu() {
        return MIN_CPU;
    }

    public int minFreeIpaCpu() {
        return MIN_FREEIPA_CPU;
    }

    public boolean suitableAsMinimumHardwareForMemory(Float memoryInGb, CdpResourceType cdpResourceType) {
        boolean suitable = false;
        if (memoryInGb != null) {
            if (CdpResourceType.FREEIPA.equals(cdpResourceType)) {
                suitable = memoryInGb >= MIN_FREEIPA_MEMORY_IN_GB;
            } else {
                suitable = memoryInGb >= MIN_MEMORY_IN_GB;
            }
        }
        return suitable;
    }

    public int minMemory() {
        return MIN_MEMORY_IN_GB;
    }

    public int minFreeIpaMemory() {
        return MIN_FREEIPA_MEMORY_IN_GB;
    }

}
