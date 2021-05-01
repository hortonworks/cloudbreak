package com.sequenceiq.cloudbreak.cloud.gcp.model;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
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

        Assert.assertEquals("1", machineDefinitionView.getId());
        Assert.assertEquals("description", machineDefinitionView.getDescription());
        Assert.assertEquals("link", machineDefinitionView.getSelfLink());
        Assert.assertEquals("time", machineDefinitionView.getCreationTimestamp());
        Assert.assertEquals("cpu", machineDefinitionView.getGuestCpus());
        Assert.assertEquals("kind", machineDefinitionView.getKind());
        Assert.assertEquals("1", machineDefinitionView.getMaximumPersistentDisks());
        Assert.assertEquals("1", machineDefinitionView.getMaximumPersistentDisksSizeGb());
        Assert.assertEquals(Integer.valueOf(1), machineDefinitionView.getMaximumNumberWithLimit());
        Assert.assertEquals("memory", machineDefinitionView.getMemoryMb());
        Assert.assertEquals("name", machineDefinitionView.getName());
        Assert.assertEquals("price", machineDefinitionView.getPrice());
        Assert.assertEquals("zone", machineDefinitionView.getZone());
    }

}