package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InstanceStoreMetadataTest {

    @Test
    public void testEmpty() {
        InstanceStoreMetadata instanceStoreMetadata = new InstanceStoreMetadata();

        Assertions.assertNull(instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("instance"));
        Assertions.assertEquals(0, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("instance"));
    }

    @Test
    public void testConstructorCreated() {
        Map<String, Integer> testMap = new HashMap<>();
        testMap.put("instance1", 1);
        testMap.put("instance2", 2);

        InstanceStoreMetadata instanceStoreMetadata = new InstanceStoreMetadata(testMap);

        Assertions.assertEquals(1, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("instance1"));
        Assertions.assertEquals(0, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("instance3"));
    }

    @Test
    public void testAddingSameEntries() {
        InstanceStoreMetadata instanceStoreMetadata = new InstanceStoreMetadata();
        instanceStoreMetadata.addInstanceStoreCountToInstanceType("instance1", 1);

        Assertions.assertEquals(1, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("instance1"));
        Assertions.assertEquals(1, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("instance1"));

        instanceStoreMetadata.addInstanceStoreCountToInstanceType("instance1", 2);

        Assertions.assertEquals(2, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("instance1"));
        Assertions.assertEquals(2, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("instance1"));

    }
}
