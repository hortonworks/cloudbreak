package com.sequenceiq.cloudbreak.orchestrator.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GrainPropertiesTest {

    private GrainProperties underTest;

    @BeforeEach
    public void init() {
        underTest = new GrainProperties();
    }

    @Test
    void whenPropertyIsEmptyThenHostsPerGrainMapEmpty() {
        Map<Entry<String, String>, Collection<String>> hostsPerGrainMap = underTest.getHostsPerGrainMap();
        assertTrue(hostsPerGrainMap.isEmpty());
    }

    @Test
    void testWhenEveryHostsHaveDifferentGrain() {
        underTest.put("HOST1", Map.of("GRAINKEY1", "GRAINVALUE1"));
        underTest.put("HOST2", Map.of("GRAINKEY2", "GRAINVALUE2"));
        Map<Entry<String, String>, Collection<String>> hostsPerGrainMap = underTest.getHostsPerGrainMap();
        assertEquals(2, hostsPerGrainMap.size());
        assertTrue(hostsPerGrainMap.containsKey(Map.entry("GRAINKEY1", "GRAINVALUE1")));
        assertTrue(hostsPerGrainMap.containsKey(Map.entry("GRAINKEY2", "GRAINVALUE2")));
        assertEquals(List.of("HOST1"), hostsPerGrainMap.get(Map.entry("GRAINKEY1", "GRAINVALUE1")));
        assertEquals(List.of("HOST2"), hostsPerGrainMap.get(Map.entry("GRAINKEY2", "GRAINVALUE2")));
    }

    @Test
    void testWhenEveryHostsHaveDifferentGrainValue() {
        underTest.put("HOST1", Map.of("GRAINKEY1", "GRAINVALUE1"));
        underTest.put("HOST2", Map.of("GRAINKEY1", "GRAINVALUE2"));
        Map<Entry<String, String>, Collection<String>> hostsPerGrainMap = underTest.getHostsPerGrainMap();
        assertEquals(2, hostsPerGrainMap.size());
        assertTrue(hostsPerGrainMap.containsKey(Map.entry("GRAINKEY1", "GRAINVALUE1")));
        assertTrue(hostsPerGrainMap.containsKey(Map.entry("GRAINKEY1", "GRAINVALUE2")));
        assertEquals(List.of("HOST1"), hostsPerGrainMap.get(Map.entry("GRAINKEY1", "GRAINVALUE1")));
        assertEquals(List.of("HOST2"), hostsPerGrainMap.get(Map.entry("GRAINKEY1", "GRAINVALUE2")));
    }

    @Test
    void testWhenEveryHostsHaveDifferentGrainKey() {
        underTest.put("HOST1", Map.of("GRAINKEY1", "GRAINVALUE1"));
        underTest.put("HOST2", Map.of("GRAINKEY2", "GRAINVALUE1"));
        Map<Entry<String, String>, Collection<String>> hostsPerGrainMap = underTest.getHostsPerGrainMap();
        assertEquals(2, hostsPerGrainMap.size());
        assertTrue(hostsPerGrainMap.containsKey(Map.entry("GRAINKEY1", "GRAINVALUE1")));
        assertTrue(hostsPerGrainMap.containsKey(Map.entry("GRAINKEY2", "GRAINVALUE1")));
        assertEquals(List.of("HOST1"), hostsPerGrainMap.get(Map.entry("GRAINKEY1", "GRAINVALUE1")));
        assertEquals(List.of("HOST2"), hostsPerGrainMap.get(Map.entry("GRAINKEY2", "GRAINVALUE1")));
    }

    @Test
    void testWhenEveryHostsHaveSameGrain() {
        underTest.put("HOST1", Map.of("GRAINKEY1", "GRAINVALUE1"));
        underTest.put("HOST2", Map.of("GRAINKEY1", "GRAINVALUE1"));
        Map<Entry<String, String>, Collection<String>> hostsPerGrainMap = underTest.getHostsPerGrainMap();
        assertEquals(1, hostsPerGrainMap.size());
        assertTrue(hostsPerGrainMap.containsKey(Map.entry("GRAINKEY1", "GRAINVALUE1")));
        assertEquals(List.of("HOST2", "HOST1"), hostsPerGrainMap.get(Map.entry("GRAINKEY1", "GRAINVALUE1")));
    }

    @Test
    void testComplexCase() {
        underTest.put("HOST1", Map.of("GRAINKEY1", "GRAINVALUE1"));
        underTest.put("HOST2", Map.of("GRAINKEY1", "GRAINVALUE1"));
        underTest.put("HOST3", Map.of("GRAINKEY3", "GRAINVALUE3"));
        underTest.put("HOST4", Map.of("GRAINKEY3", "GRAINVALUE4"));
        underTest.put("HOST5", Map.of("GRAINKEY5", "GRAINVALUE4"));
        Map<Entry<String, String>, Collection<String>> hostsPerGrainMap = underTest.getHostsPerGrainMap();
        assertEquals(4, hostsPerGrainMap.size());
        assertTrue(hostsPerGrainMap.containsKey(Map.entry("GRAINKEY1", "GRAINVALUE1")));
        assertEquals(List.of("HOST2", "HOST1"), hostsPerGrainMap.get(Map.entry("GRAINKEY1", "GRAINVALUE1")));
        assertEquals(List.of("HOST3"), hostsPerGrainMap.get(Map.entry("GRAINKEY3", "GRAINVALUE3")));
        assertEquals(List.of("HOST4"), hostsPerGrainMap.get(Map.entry("GRAINKEY3", "GRAINVALUE4")));
        assertEquals(List.of("HOST5"), hostsPerGrainMap.get(Map.entry("GRAINKEY5", "GRAINVALUE4")));
    }

    @Test
    void testMultipleGrainPerHostCase() {
        underTest.put("HOST1", Map.of("GRAINKEY1", "GRAINVALUE1"));
        underTest.put("HOST2", Map.of("GRAINKEY1", "GRAINVALUE1", "GRAINKEY2", "GRAINVALUE2"));
        underTest.put("HOST3", Map.of("GRAINKEY3", "GRAINVALUE3"));
        underTest.put("HOST4", Map.of("GRAINKEY3", "GRAINVALUE4"));
        underTest.put("HOST5", Map.of("GRAINKEY5", "GRAINVALUE4", "GRAINKEY2", "GRAINVALUE2"));
        Map<Entry<String, String>, Collection<String>> hostsPerGrainMap = underTest.getHostsPerGrainMap();
        assertEquals(5, hostsPerGrainMap.size());
        assertTrue(hostsPerGrainMap.containsKey(Map.entry("GRAINKEY1", "GRAINVALUE1")));
        assertEquals(List.of("HOST2", "HOST1"), hostsPerGrainMap.get(Map.entry("GRAINKEY1", "GRAINVALUE1")));
        assertEquals(List.of("HOST2", "HOST5"), hostsPerGrainMap.get(Map.entry("GRAINKEY2", "GRAINVALUE2")));
        assertEquals(List.of("HOST3"), hostsPerGrainMap.get(Map.entry("GRAINKEY3", "GRAINVALUE3")));
        assertEquals(List.of("HOST4"), hostsPerGrainMap.get(Map.entry("GRAINKEY3", "GRAINVALUE4")));
        assertEquals(List.of("HOST5"), hostsPerGrainMap.get(Map.entry("GRAINKEY5", "GRAINVALUE4")));
    }
}