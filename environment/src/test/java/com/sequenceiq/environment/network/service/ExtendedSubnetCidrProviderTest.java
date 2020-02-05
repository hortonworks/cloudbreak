package com.sequenceiq.environment.network.service;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExtendedSubnetCidrProviderTest {

    private static final int NUMBER_OF_SUBNETS = 6;

    private ExtendedSubnetCidrProvider underTest = new ExtendedSubnetCidrProvider();

    @Test
    void testProvideShouldCreateSubnets() {
        String networkCidr = "10.10.0.0/16";

        Set<String> actual = underTest.provide(networkCidr);

        Assertions.assertEquals(NUMBER_OF_SUBNETS, actual.size());
        Assertions.assertTrue(actual.contains("10.10.0.0/19"));
        Assertions.assertTrue(actual.contains("10.10.32.0/19"));
        Assertions.assertTrue(actual.contains("10.10.64.0/19"));
        Assertions.assertTrue(actual.contains("10.10.96.0/19"));
        Assertions.assertTrue(actual.contains("10.10.128.0/19"));
        Assertions.assertTrue(actual.contains("10.10.160.0/19"));
    }

}