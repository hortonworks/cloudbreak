package com.sequenceiq.cloudbreak.cloud.gcp.conf;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GcpInstanceTypeHyperDiskConfigTest {
    @Test
    void testGetFamilyConfigWhenEmpty() {
        GcpInstanceTypeHyperDiskConfig underTest = new GcpInstanceTypeHyperDiskConfig(new HashMap<>(), new HashMap<>());
        underTest.init();

        assertTrue(underTest.getInstanceTypeConfig("anything").isEmpty());
        assertTrue(underTest.getFamilyConfig("anything").isEmpty());
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

        assertTrue(underTest.getInstanceTypeConfig("anything").isEmpty());
        assertTrue(underTest.getFamilyConfig("anything").isEmpty());
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

        assertFalse(underTest.isHyperdiskBalancedSupportedForInstanceType("anything"));
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

        assertTrue(underTest.isHyperdiskBalancedSupportedForInstanceType("f1-t1"));
    }

    @Test
    void testIsHyperdiskBalancedSupportedForAllInstanceType() {
        GcpInstanceTypeHyperDiskConfig.InstanceFamilyConfig hyperAndNormalFamily = Mockito.mock(GcpInstanceTypeHyperDiskConfig.InstanceFamilyConfig.class);
        Mockito.when(hyperAndNormalFamily.hyperDiskBalancedSupported()).thenReturn(true);
        Mockito.when(hyperAndNormalFamily.pdBalancedSupported()).thenReturn(true);
        GcpInstanceTypeHyperDiskConfig.InstanceFamilyConfig onlyHyperFamily = Mockito.mock(GcpInstanceTypeHyperDiskConfig.InstanceFamilyConfig.class);
        Mockito.when(onlyHyperFamily.hyperDiskBalancedSupported()).thenReturn(true);
        GcpInstanceTypeHyperDiskConfig underTest = new GcpInstanceTypeHyperDiskConfig(
                Map.ofEntries(
                        Map.entry("c4d", onlyHyperFamily),
                        Map.entry("c3d", hyperAndNormalFamily)),
                Map.ofEntries(
                        Map.entry("c3d-standard-16", Mockito.mock(GcpInstanceTypeHyperDiskConfig.InstanceTypeConfig.class)),
                        Map.entry("c3d-highcpu-16", Mockito.mock(GcpInstanceTypeHyperDiskConfig.InstanceTypeConfig.class))
                ));
        underTest.init();
        Pair<Boolean, Boolean> result = underTest.isHyperdiskBalancedSupportedForAllInstanceType(List.of("c3d-standard-16", "c2d-standard-16"));
        Pair<Boolean, Boolean> resultHyper = underTest.isHyperdiskBalancedSupportedForAllInstanceType(List.of("c3d-standard-16", "c3d-highcpu-16"));
        Pair<Boolean, Boolean> resultOnlyHyper = underTest.isHyperdiskBalancedSupportedForAllInstanceType(List.of("c4d-standard-16", "c4d-highcpu-16"));
        Pair<Boolean, Boolean> resultUnresolvable = underTest.isHyperdiskBalancedSupportedForAllInstanceType(List.of("c4d-standard-16", "c2d-standard-16"));

        assertFalse(result.getLeft());
        assertTrue(result.getRight());
        assertTrue(resultHyper.getLeft());
        assertTrue(resultHyper.getRight());
        assertTrue(resultOnlyHyper.getLeft());
        assertFalse(resultOnlyHyper.getRight());
        assertFalse(resultUnresolvable.getLeft());
        assertFalse(resultUnresolvable.getRight());
    }
}