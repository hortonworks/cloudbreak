package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.network.CloudSubnet;
import com.sequenceiq.cloudbreak.domain.environment.Subnet;
import com.sequenceiq.cloudbreak.domain.environment.SubnetVisibility;

public class SubnetConverterTest {

    private static final String CIDR = "1.1.0.0/8";

    private static final String AVAILABILITY_ZONE = "az";

    private static final String SUBNET_ID = "subnet-1";

    private SubnetConverter underTest = new SubnetConverter();

    @Test
    public void testConvert() {
        Set<CloudSubnet> cloudSubnets = createCloudSubnets();

        Set<Subnet> actual = underTest.convert(cloudSubnets);

        Subnet actualSubnet = actual.iterator().next();
        Assert.assertEquals(CIDR, actualSubnet.getCidr());
        Assert.assertEquals(AVAILABILITY_ZONE, actualSubnet.getAvailabilityZone());
        Assert.assertEquals(SUBNET_ID, actualSubnet.getSubnetId());
        Assert.assertEquals(SubnetVisibility.PUBLIC, actualSubnet.getVisibility());
    }

    @Test
    public void testConvertWhenTheParameterIsEmpty() {
        Set<Subnet> actual = underTest.convert(Collections.emptySet());

        Assert.assertEquals(Collections.emptySet(), actual);
    }

    private Set<CloudSubnet> createCloudSubnets() {
        CloudSubnet cloudSubnet = new CloudSubnet();
        cloudSubnet.setCidr(CIDR);
        cloudSubnet.setAvailabilityZone(AVAILABILITY_ZONE);
        cloudSubnet.setSubnetId(SUBNET_ID);
        cloudSubnet.setPrivateSubnet(false);
        return Set.of(cloudSubnet);
    }

}