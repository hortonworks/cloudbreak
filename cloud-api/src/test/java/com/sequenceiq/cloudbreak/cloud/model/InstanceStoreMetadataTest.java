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
        Map<String, VolumeParameterConfig> testMap = new HashMap<>();
        VolumeParameterConfig firstConfig = new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1);
        VolumeParameterConfig secondConfig = new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 2, 2);
        testMap.put("instance1", firstConfig);
        testMap.put("instance2", secondConfig);

        InstanceStoreMetadata instanceStoreMetadata = new InstanceStoreMetadata(testMap);

        Assertions.assertEquals(1, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("instance1"));
        Assertions.assertEquals(0, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("instance3"));
    }

    @Test
    public void testAddingSameEntries() {
        InstanceStoreMetadata instanceStoreMetadata = new InstanceStoreMetadata();
        VolumeParameterConfig singleStorage = new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1);
        instanceStoreMetadata.addInstanceStoreConfigToInstanceType("instance1", singleStorage);

        Assertions.assertEquals(1, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("instance1"));
        Assertions.assertEquals(1, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("instance1"));

        VolumeParameterConfig multipleStorage = new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 2, 2);
        instanceStoreMetadata.addInstanceStoreConfigToInstanceType("instance1", multipleStorage);

        Assertions.assertEquals(2, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("instance1"));
        Assertions.assertEquals(2, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("instance1"));

    }
}
