package com.sequenceiq.it.cloudbreak.constants;

public final class NetworkConstants {

    private NetworkConstants() {
    }

    public enum NetworkConfig {

        OPEN_NETWORK("0.0.0.0/0"),
        SUBNET_8("10.0.0.0/8"),
        SUBNET_16("10.0.0.0/16");

        private final String cidr;

        NetworkConfig(String cidr) {
            this.cidr = cidr;
        }

        public String getCidr() {
            return cidr;
        }

    }

}
