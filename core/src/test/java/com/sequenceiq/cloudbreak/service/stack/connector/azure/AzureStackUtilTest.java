package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AzureStackUtilTest {

    @InjectMocks
    private AzureStackUtil util;

    @Test
    public void testGetNextIPAddress() {
        String startIp = "172.16.0.253";

        String next1 = util.getNextIPAddress(startIp);
        String next2 = util.getNextIPAddress(next1);
        String next3 = util.getNextIPAddress(next2);

        assertEquals("172.16.0.254", next1);
        assertEquals("172.16.0.255", next2);
        assertEquals("172.16.1.0", next3);
    }


    @Test
    public void testGetFirstAssignableIpOfSubnet() {
        String subnetCIDR16 = "10.0.0.0/16";
        String otherSubnetCIDR16 = "10.0.3.4/16";
        String subnetCIDR24 = "10.0.5.0/24";
        String expectedForCIDR16 = "10.0.0.4";
        String expectedForOtherCIDR16 = "10.0.0.4";
        String expectedForCIDR24 = "10.0.5.4";

        String actualForCIDR16 = util.getFirstAssignableIPOfSubnet(subnetCIDR16);
        String actualForOtherCIDR16 = util.getFirstAssignableIPOfSubnet(otherSubnetCIDR16);
        String actualForCIDR24 = util.getFirstAssignableIPOfSubnet(subnetCIDR24);

        assertEquals(expectedForCIDR16, actualForCIDR16);
        assertEquals(expectedForOtherCIDR16, actualForOtherCIDR16);
        assertEquals(expectedForCIDR24, actualForCIDR24);
    }

    @Test
    public void testGetLastAssignableIpOfSubnet() {
        String subnetCIDR16 = "10.0.0.0/16";
        String otherSubnetCIDR16 = "10.0.3.4/16";
        String subnetCIDR24 = "10.0.5.0/24";
        String expectedForCIDR16 = "10.0.255.254";
        String expectedForOtherCIDR16 = "10.0.255.254";
        String expectedForCIDR24 = "10.0.5.254";

        String actualForCIDR16 = util.getLastAssignableIPOfSubnet(subnetCIDR16);
        String actualForOtherCIDR16 = util.getLastAssignableIPOfSubnet(otherSubnetCIDR16);
        String actualForCIDR24 = util.getLastAssignableIPOfSubnet(subnetCIDR24);

        assertEquals(expectedForCIDR16, actualForCIDR16);
        assertEquals(expectedForOtherCIDR16, actualForOtherCIDR16);
        assertEquals(expectedForCIDR24, actualForCIDR24);
    }
}
