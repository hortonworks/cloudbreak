package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class SubnetCidrProviderTest {

    private SubnetCidrProvider underTest = new SubnetCidrProvider();

    @Test
    public void testProvide() {

        Set<String> actual = underTest.provide("10.0.0.0/16");

        Assert.assertTrue(actual.contains("10.0.1.0/24"));
        Assert.assertTrue(actual.contains("10.0.2.0/24"));
        Assert.assertTrue(actual.contains("10.0.3.0/24"));
    }

}