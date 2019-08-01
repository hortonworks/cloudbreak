package com.sequenceiq.environment.network.service;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExtendedSubnetCidrProviderTest {

    private ExtendedSubnetCidrProvider underTest = new ExtendedSubnetCidrProvider();

    @Test
    void testProvideShouldCreateSubnets() {
        String networkCidr = "10.10.0.0/16";

        Set<String> actual = underTest.provide(networkCidr);

        Assertions.assertTrue(actual.contains("10.10.0.0/19"));
        Assertions.assertTrue(actual.contains("10.10.32.0/19"));
        Assertions.assertTrue(actual.contains("10.10.64.0/19"));
    }

}