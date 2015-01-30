package com.sequenceiq.cloudbreak.domain;

public enum AwsInstanceType {

    T1Micro("t1.micro", 0),
    T2Micro("t2.micro", 0),
    T2Small("t2.small", 0),
    T2Medium("t2.medium", 0),

    M1Small("m1.small", 1),
    M1Medium("m1.medium", 1),
    M1Large("m1.large", 2),
    M1Xlarge("m1.xlarge", 4),
    M3Medium("m3.medium", 1),
    M3Large("m3.large", 1),
    M3Xlarge("m3.xlarge", 2),
    M32xlarge("m3.2xlarge", 2),
    M2Xlarge("m2.xlarge", 1),
    M22xlarge("m2.2xlarge", 1),
    M24xlarge("m2.4xlarge", 2),

    I2Xlarge("i2.xlarge", 1),
    I22xlarge("i2.2xlarge", 2),
    I24xlarge("i2.4xlarge", 4),
    I28xlarge("i2.8xlarge", 8),

    Hi14xlarge("hi1.4xlarge", 2),
    Hs18xlarge("hs1.8xlarge", 24),

    Cr18xlarge("cr1.8xlarge", 2),
    C1Medium("c1.medium", 1),
    C1Xlarge("c1.xlarge", 4),
    C3Large("c3.large", 2),
    C3Xlarge("c3.xlarge", 2),
    C32xlarge("c3.2xlarge", 2),
    C34xlarge("c3.4xlarge", 2),
    C38xlarge("c3.8xlarge", 2),
    Cc28xlarge("cc2.8xlarge", 4),
    Cg14xlarge("cg1.4xlarge", 2),

    G22xlarge("g2.2xlarge", 1),

    R3Large("r3.large", 1),
    R3Xlarge("r3.xlarge", 1),
    R32xlarge("r3.2xlarge", 1),
    R34xlarge("r3.4xlarge", 1),
    R38xlarge("r3.8xlarge", 2);

    private String value;
    private int ephemeralVolumes;

    private AwsInstanceType(String value, int ephemeralVolumes) {
        this.value = value;
        this.ephemeralVolumes = ephemeralVolumes;
    }

    public String getValue() {
        return value;
    }

    public int getEphemeralVolumes() {
        return ephemeralVolumes;
    }

    @Override
    public String toString() {
        return value;
    }
}