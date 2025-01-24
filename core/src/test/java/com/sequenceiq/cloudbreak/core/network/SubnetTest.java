package com.sequenceiq.cloudbreak.core.network;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;

public class SubnetTest {

    protected static final String AZ_1 = "AZ-1";

    protected static final String AZ_2 = "AZ-2";

    protected static final String PRIVATE_ID_1 = "private-id-1";

    protected static final String PRIVATE_ID_2 = "private-id-2";

    protected static final String PUBLIC_ID_1 = "public-id-1";

    protected CloudSubnet getPublicCloudSubnet(String id, String availabilityZone) {
        return new CloudSubnet.Builder()
                .id(id)
                .name("name")
                .availabilityZone(availabilityZone)
                .cidr("cidr")
                .privateSubnet(false)
                .mapPublicIpOnLaunch(false)
                .igwAvailable(true)
                .type(SubnetType.PUBLIC)
                .build();
    }

    protected CloudSubnet getPrivateCloudSubnet(String id, String availabilityZone) {
        return new CloudSubnet.Builder()
                .id(id)
                .name("name")
                .availabilityZone(availabilityZone)
                .cidr("cidr")
                .privateSubnet(true)
                .mapPublicIpOnLaunch(false)
                .igwAvailable(false)
                .type(SubnetType.PRIVATE)
                .build();
    }
}
