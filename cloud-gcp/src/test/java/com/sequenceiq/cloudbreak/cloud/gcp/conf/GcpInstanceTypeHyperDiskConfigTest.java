package com.sequenceiq.cloudbreak.cloud.gcp.conf;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GcpInstanceTypeHyperDiskConfigTest {
    @Test
    void testGetFamilyConfigWhenEmpty() {
        GcpInstanceTypeHyperDiskConfig underTest = new GcpInstanceTypeHyperDiskConfig(new HashMap<>(), new HashMap<>());
        underTest.init();

        Assertions.assertTrue(underTest.getInstanceTypeConfig("anything").isEmpty());
        Assertions.assertTrue(underTest.getFamilyConfig("anything").isEmpty());
    }

    @Test
    void testGetFamilyConfigWhenNoMatch() {
        GcpInstanceTypeHyperDiskConfig underTest = new GcpInstanceTypeHyperDiskConfig(
                Map.ofEntries(
                        Map.entry("f1-t1", Mockito.mock(GcpInstanceTypeHyperDiskConfig.InstanceFamilyConfig.class)),
                        Map.entry("f2-t1", Mockito.mock(GcpInstanceTypeHyperDiskConfig.InstanceFamilyConfig.class))),
                Map.ofEntries(
                        Map.entry("f1-t1", Mockito.mock(GcpInstanceTypeHyperDiskConfig.InstanceTypeConfig.class)),
                        Map.entry("f2-t1", Mockito.mock(GcpInstanceTypeHyperDiskConfig.InstanceTypeConfig.class))));
        underTest.init();

        Assertions.assertTrue(underTest.getInstanceTypeConfig("anything").isEmpty());
        Assertions.assertTrue(underTest.getFamilyConfig("anything").isEmpty());
    }

    @Test
    void testGetFamilyConfigWhenMatch() {
        GcpInstanceTypeHyperDiskConfig.InstanceFamilyConfig expectedFamily = Mockito.mock(GcpInstanceTypeHyperDiskConfig.InstanceFamilyConfig.class);
        GcpInstanceTypeHyperDiskConfig.InstanceTypeConfig expectedType = Mockito.mock(GcpInstanceTypeHyperDiskConfig.InstanceTypeConfig.class);
        GcpInstanceTypeHyperDiskConfig underTest = new GcpInstanceTypeHyperDiskConfig(
                Map.ofEntries(
                        Map.entry("f1", expectedFamily),
                        Map.entry("f2", Mockito.mock(GcpInstanceTypeHyperDiskConfig.InstanceFamilyConfig.class))),
                Map.ofEntries(
                        Map.entry("f1-t1", expectedType),
                        Map.entry("f2-t1", Mockito.mock(GcpInstanceTypeHyperDiskConfig.InstanceTypeConfig.class))));
        underTest.init();

        Assertions.assertEquals(expectedType, underTest.getInstanceTypeConfig("f1-t1").get());
        Assertions.assertEquals(expectedFamily, underTest.getFamilyConfig("f1-t1").get());
    }

    @Test
    void testIsHyperdiskBalancedSupportedForInstanceTypeWhenEmpty() {
        GcpInstanceTypeHyperDiskConfig underTest = new GcpInstanceTypeHyperDiskConfig(new HashMap<>(), new HashMap<>());
        underTest.init();

        Assertions.assertFalse(underTest.isHyperdiskBalancedSupportedForInstanceType("anything"));
    }

    @Test
    void testIsHyperdiskBalancedSupportedForInstanceTypeWhenMatch() {
        GcpInstanceTypeHyperDiskConfig.InstanceFamilyConfig expectedFamily = Mockito.mock(GcpInstanceTypeHyperDiskConfig.InstanceFamilyConfig.class);
        Mockito.when(expectedFamily.hyperDiskBalancedSupported()).thenReturn(true);
        GcpInstanceTypeHyperDiskConfig.InstanceTypeConfig expectedType = Mockito.mock(GcpInstanceTypeHyperDiskConfig.InstanceTypeConfig.class);
        GcpInstanceTypeHyperDiskConfig underTest = new GcpInstanceTypeHyperDiskConfig(
                Map.ofEntries(
                        Map.entry("f1", expectedFamily),
                        Map.entry("f2", Mockito.mock(GcpInstanceTypeHyperDiskConfig.InstanceFamilyConfig.class))),
                Map.ofEntries(
                        Map.entry("f1-t1", expectedType),
                        Map.entry("f2-t1", Mockito.mock(GcpInstanceTypeHyperDiskConfig.InstanceTypeConfig.class))));
        underTest.init();

        Assertions.assertTrue(underTest.isHyperdiskBalancedSupportedForInstanceType("f1-t1"));
    }
}