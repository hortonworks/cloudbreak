package com.sequenceiq.cloudbreak.core.flow2.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class NetworkScaleDetailsTest {

    @Test
    public void testDefaultConstructor() {
        NetworkScaleDetails networkScaleDetails = new NetworkScaleDetails();
        assertNotNull(networkScaleDetails);
        List<String> subnetIds = networkScaleDetails.getPreferredSubnetIds();
        Set<String> availabilityZones = networkScaleDetails.getPreferredAvailabilityZones();
        assertNotNull(subnetIds);
        assertNotNull(availabilityZones);
        assertEquals(0, subnetIds.size());
        assertEquals(0, availabilityZones.size());
    }

    @Test
    public void testParameterizedConstructor() {
        List<String> subnetIds = new ArrayList<>();
        subnetIds.add("subnet-1");
        Set<String> availabilityZones = new HashSet<>();
        availabilityZones.add("us-east-1");

        NetworkScaleDetails networkScaleDetails = new NetworkScaleDetails(subnetIds, availabilityZones);
        assertNotNull(networkScaleDetails);

        List<String> subnetIdsResult = networkScaleDetails.getPreferredSubnetIds();
        Set<String> availabilityZonesResult = networkScaleDetails.getPreferredAvailabilityZones();

        assertNotNull(subnetIdsResult);
        assertNotNull(availabilityZonesResult);

        assertEquals(subnetIds, subnetIdsResult);
        assertEquals(availabilityZones, availabilityZonesResult);
    }

    @Test
    public void testGetEmpty() {
        NetworkScaleDetails networkScaleDetails = NetworkScaleDetails.getEmpty();
        assertNotNull(networkScaleDetails);

        List<String> subnetIds = networkScaleDetails.getPreferredSubnetIds();
        Set<String> availabilityZones = networkScaleDetails.getPreferredAvailabilityZones();

        assertNotNull(subnetIds);
        assertNotNull(availabilityZones);
        assertEquals(0, subnetIds.size());
        assertEquals(0, availabilityZones.size());
    }

}