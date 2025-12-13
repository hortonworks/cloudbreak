package com.sequenceiq.cloudbreak.cloud.gcp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class MachineDefinitionViewTest {

    @Test
    public void testMachineDefinitionView() {
        Map<String, Object> map = new HashMap<>();
        map.put("kind", "kind");
        map.put("description", "description");
        map.put("id", "1");
        map.put("selfLink", "link");
        map.put("creationTimestamp", "time");
        map.put("guestCpus", "cpu");
        map.put("maximumPersistentDisks", "1");
        map.put("maximumPersistentDisksSizeGb", "1");
        map.put("memoryMb", "memory");
        map.put("name", "name");
        map.put("price", "price");
        map.put("zone", "zone");

        MachineDefinitionView machineDefinitionView = new MachineDefinitionView(map);

        assertEquals("1", machineDefinitionView.getId());
        assertEquals("description", machineDefinitionView.getDescription());
        assertEquals("link", machineDefinitionView.getSelfLink());
        assertEquals("time", machineDefinitionView.getCreationTimestamp());
        assertEquals("cpu", machineDefinitionView.getGuestCpus());
        assertEquals("kind", machineDefinitionView.getKind());
        assertEquals("1", machineDefinitionView.getMaximumPersistentDisks());
        assertEquals("1", machineDefinitionView.getMaximumPersistentDisksSizeGb());
        assertEquals(Integer.valueOf(1), machineDefinitionView.getMaximumNumberWithLimit());
        assertEquals("memory", machineDefinitionView.getMemoryMb());
        assertEquals("name", machineDefinitionView.getName());
        assertEquals("price", machineDefinitionView.getPrice());
        assertEquals("zone", machineDefinitionView.getZone());
    }

}