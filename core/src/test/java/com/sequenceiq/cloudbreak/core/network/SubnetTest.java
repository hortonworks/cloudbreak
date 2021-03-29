package com.sequenceiq.cloudbreak.core.network;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;

public class SubnetTest {

    protected static final String AZ_1 = "AZ-1";

    protected static final String AZ_2 = "AZ-2";

    protected static final String PRIVATE_ID_1 = "private-id-1";

    protected static final String PUBLIC_ID_1 = "public-id-1";

    protected CloudSubnet getPublicCloudSubnet(String id, String availabilityZone) {
        return new CloudSubnet(id, "name", availabilityZone, "cidr", false, true, true, SubnetType.PUBLIC);
    }

    protected CloudSubnet getPrivateCloudSubnet(String id, String availabilityZone) {
        return new CloudSubnet(id, "name", availabilityZone, "cidr", true, false, false, SubnetType.PRIVATE);
    }

    protected CloudSubnet getRoutableToInternetCloudSubnet(String id, String availabilityZone) {
        return new CloudSubnet(id, "name", availabilityZone, "cidr", false, true, true, SubnetType.PRIVATE, true);
    }
}
