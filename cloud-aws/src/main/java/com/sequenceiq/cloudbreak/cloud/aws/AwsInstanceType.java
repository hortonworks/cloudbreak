package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.meta;

import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;

public enum AwsInstanceType {

    M3Medium("m3.medium", meta(1, "4 GB SSD")),
    M3Large("m3.large", meta(1, "32 GB SSD")),
    M3Xlarge("m3.xlarge", meta(2, "40 GB SSD")),
    M32xlarge("m3.2xlarge", meta(2, "80 GB SSD")),

    M4Large("m4.large", meta(0, "")),
    M4Xlarge("m4.xlarge", meta(0, "")),
    M42xlarge("m4.2xlarge", meta(0, "")),
    M44xlarge("m4.4xlarge", meta(0, "")),

    T2large("t2.large", meta(0, "")),

    I2Xlarge("i2.xlarge", meta(1, "800 GB SSD")),
    I22xlarge("i2.2xlarge", meta(2, "800 GB SSD")),
    I24xlarge("i2.4xlarge", meta(4, "800 GB SSD")),
    I28xlarge("i2.8xlarge", meta(8, "800 GB SSD")),

    Hi14xlarge("hi1.4xlarge", meta(2, "1024 GB SSD")),
    Hs18xlarge("hs1.8xlarge", meta(24, "2048 GB STANDARD")),

    C3Large("c3.large", meta(2, "16 GB SSD")),
    C3Xlarge("c3.xlarge", meta(2, "40 GB SSD")),
    C32xlarge("c3.2xlarge", meta(2, "80 GB SSD")),
    C34xlarge("c3.4xlarge", meta(2, "160 GB SSD")),
    C38xlarge("c3.8xlarge", meta(2, "320 GB SSD")),
    Cc28xlarge("cc2.8xlarge", meta(4, "840 GB STANDARD")),
    Cg14xlarge("cg1.4xlarge", meta(2, "840 GB STANDARD")),
    Cr18xlarge("cr1.8xlarge", meta(2, "120 GB SSD")),

    G22xlarge("g2.2xlarge", meta(1, "60 GB SSD")),

    R3Large("r3.large", meta(1, "32 GB SSD")),
    R3Xlarge("r3.xlarge", meta(1, "80 GB SSD")),
    R32xlarge("r3.2xlarge", meta(1, "160 GB SSD")),
    R34xlarge("r3.4xlarge", meta(1, "320 GB SSD")),
    R38xlarge("r3.8xlarge", meta(2, "320 GB SSD")),

    D2Xlarge("d2.xlarge", meta(3, "2000 GB STANDARD")),
    D22xlarge("d2.2xlarge", meta(6, "2000 GB STANDARD")),
    D24xlarge("d2.4xlarge", meta(12, "2000 GB STANDARD")),
    D28xlarge("d2.8xlarge", meta(24, "2000 GB STANDARD"));

    private String value;
    private VmTypeMeta meta;

    AwsInstanceType(String value, VmTypeMeta meta) {
        this.value = value;
        this.meta = meta;
    }

    public String getValue() {
        return value;
    }

    public static int getVolumeCountByType(String value) {
        for (AwsInstanceType item : AwsInstanceType.values()) {
            if (item.value.equals(value)) {
                return item.getMeta().maxEphemeralVolumeCount();
            }
        }
        throw new IllegalArgumentException(String.format("There's no '%s' aws volume type.", value));
    }

    public VmTypeMeta getMeta() {
        return meta;
    }

    @Override
    public String toString() {
        return value;
    }
}
