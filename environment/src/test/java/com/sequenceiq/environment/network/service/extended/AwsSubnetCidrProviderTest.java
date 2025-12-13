package com.sequenceiq.environment.network.service.extended;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.network.NetworkSubnetRequest;
import com.sequenceiq.environment.network.service.Cidrs;

class AwsSubnetCidrProviderTest {

    private static final int NUMBER_OF_PRIVATE_SUBNETS = 3;

    private static final int NUMBER_OF_PUBLIC_SUBNETS = 3;

    private AwsSubnetCidrProvider underTest = new AwsSubnetCidrProvider(new ExtendedSubnetTypeProvider());

    @Test
    void testProvideShouldCreateSubnets() {
        String networkCidr = "10.10.0.0/16";

        Cidrs actual = underTest.provide(networkCidr, true);

        assertEquals(NUMBER_OF_PRIVATE_SUBNETS, actual.getPrivateSubnets().size());
        assertEquals(NUMBER_OF_PUBLIC_SUBNETS, actual.getPublicSubnets().size());

        assertTrue(getCidrs(actual.getPublicSubnets()).contains("10.10.0.0/24"));
        assertTrue(getCidrs(actual.getPublicSubnets()).contains("10.10.1.0/24"));
        assertTrue(getCidrs(actual.getPublicSubnets()).contains("10.10.2.0/24"));

        assertTrue(getCidrs(actual.getPrivateSubnets()).contains("10.10.160.0/19"));
        assertTrue(getCidrs(actual.getPrivateSubnets()).contains("10.10.192.0/19"));
        assertTrue(getCidrs(actual.getPrivateSubnets()).contains("10.10.224.0/19"));
    }

    private Set<String> getCidrs(Set<NetworkSubnetRequest> networkSubnetRequests) {
        return networkSubnetRequests.stream().map(NetworkSubnetRequest::getCidr).collect(Collectors.toSet());
    }
}