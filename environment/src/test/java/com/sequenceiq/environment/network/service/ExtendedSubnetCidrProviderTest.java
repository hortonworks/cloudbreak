package com.sequenceiq.environment.network.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExtendedSubnetCidrProviderTest {

    private static final int NUMBER_OF_PRIVATE_SUBNETS = 35;

    private static final int NUMBER_OF_PUBLIC_SUBNETS = 3;

    private ExtendedSubnetCidrProvider underTest = new ExtendedSubnetCidrProvider();

    @Test
    void testProvideShouldCreateSubnets() {
        String networkCidr = "10.10.0.0/16";

        Cidrs actual = underTest.provide(networkCidr);

        Assertions.assertEquals(NUMBER_OF_PRIVATE_SUBNETS, actual.getPrivateSubnetCidrs().size());
        Assertions.assertEquals(NUMBER_OF_PUBLIC_SUBNETS, actual.getPublicSubnetCidrs().size());

        Assertions.assertTrue(actual.getPublicSubnetCidrs().contains("10.10.0.0/24"));
        Assertions.assertTrue(actual.getPublicSubnetCidrs().contains("10.10.1.0/24"));
        Assertions.assertTrue(actual.getPublicSubnetCidrs().contains("10.10.2.0/24"));

        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.3.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.4.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.5.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.6.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.7.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.8.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.9.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.10.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.11.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.12.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.13.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.14.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.15.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.16.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.17.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.18.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.19.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.20.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.21.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.22.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.23.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.24.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.25.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.26.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.27.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.28.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.29.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.30.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.31.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.32.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.33.0/24"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.34.0/24"));

        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.64.0/19"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.96.0/19"));
        Assertions.assertTrue(actual.getPrivateSubnetCidrs().contains("10.10.128.0/19"));
    }

}