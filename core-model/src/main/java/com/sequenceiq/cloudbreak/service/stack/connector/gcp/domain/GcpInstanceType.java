package com.sequenceiq.cloudbreak.service.stack.connector.gcp.domain;

public enum GcpInstanceType {

    N1_STANDARD_1("n1-standard-1"),
    N1_STANDARD_2("n1-standard-2"),
    N1_STANDARD_4("n1-standard-4"),
    N1_STANDARD_8("n1-standard-8"),
    N1_STANDARD_16("n1-standard-16"),
    N1_HIGHMEM_2("n1-highmem-2"),
    N1_HIGHMEM_4("n1-highmem-4"),
    N1_HIGHMEM_8("n1-highmem-8"),
    N1_HIGHMEM_16("n1-highmem-16"),
    N1_HIGHCPU_2("n1-highcpu-2"),
    N1_HIGHCPU_4("n1-highcpu-4"),
    N1_HIGHCPU_8("n1-highcpu-8"),
    N1_HIGHCPU_16("n1-highcpu-16"),
    F1_MICRO("f1-micro"),
    G1_SMALL("g1-small");

    private final String value;

    private GcpInstanceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
