package com.sequenceiq.cloudbreak.shell.model;

public class AvailabilitySetEntry {

    private String name;

    private AvailabilitySetFaultDomainNumber faultDomainCount;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AvailabilitySetFaultDomainNumber getFaultDomainCount() {
        return faultDomainCount;
    }

    public void setFaultDomainCount(AvailabilitySetFaultDomainNumber faultDomainCount) {
        this.faultDomainCount = faultDomainCount;
    }
}
