package com.sequenceiq.cloudbreak.shell.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AvailabilitySetFaultDomainNumber {

    TWO(2),
    THREE(3);

    private final int number;

    AvailabilitySetFaultDomainNumber(int number) {
        this.number = number;
    }

    @JsonValue
    public int number() {
        return number;
    }
}


