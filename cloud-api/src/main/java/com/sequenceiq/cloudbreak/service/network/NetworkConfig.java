package com.sequenceiq.cloudbreak.service.network;

public final class NetworkConfig {
    public static final String OPEN_NETWORK = "0.0.0.0/0";
    public static final String SUBNET_8 = "10.0.0.0/8";
    public static final String SUBNET_16 = "10.0.0.0/16";

    private NetworkConfig() {
        throw new IllegalStateException();
    }
}
