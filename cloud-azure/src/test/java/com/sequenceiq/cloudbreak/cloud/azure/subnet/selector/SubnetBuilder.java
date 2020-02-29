package com.sequenceiq.cloudbreak.cloud.azure.subnet.selector;

import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PUBLIC;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;

public class SubnetBuilder {

    static final String AZ_A = "AZ-a";

    static final String AZ_B = "AZ-b";

    static final String AZ_C = "AZ-c";

    static final String AZ_D = "AZ-d";

    static final String SUBNET_1 = "subnet-1";

    static final String SUBNET_2 = "subnet-2";

    static final String SUBNET_3 = "subnet-3";

    static final String SUBNET_4 = "subnet-4";

    private int nextId = 1;

    private final List<CloudSubnet> subnets = new ArrayList<>();

    SubnetBuilder withPrivateSubnet() {
        subnets.add(getSubnet(AZ_A, true, false));
        return this;
    }

    SubnetBuilder withPublicSubnet() {
        subnets.add(getSubnet(AZ_A, false, true));
        return this;
    }

    SubnetBuilder withPublicSubnetNoPublicIp() {
        subnets.add(getSubnet(AZ_A, false, false));
        return this;
    }

    SubnetBuilder withPrivateSubnet(String az) {
        subnets.add(getSubnet(az, true, false));
        return this;
    }

    SubnetBuilder withPublicSubnetNoPublicIp(String az) {
        subnets.add(getSubnet(az, false, false));
        return this;
    }

    SubnetBuilder withPublicSubnet(String az) {
        subnets.add(getSubnet(az, false, true));
        return this;
    }

    public List<CloudSubnet> build() {
        return subnets;
    }

    private CloudSubnet getSubnet(String az, boolean privateSubnet, boolean mapPublicOnLaunch) {
        String nextSubnetId = "subnet-" + nextId++;
        return new CloudSubnet(nextSubnetId, "", az, "", privateSubnet, mapPublicOnLaunch, !privateSubnet, PUBLIC);
    }
}
