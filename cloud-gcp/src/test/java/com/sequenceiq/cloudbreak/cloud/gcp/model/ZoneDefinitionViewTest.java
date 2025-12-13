package com.sequenceiq.cloudbreak.cloud.gcp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ZoneDefinitionViewTest {

    @Test
    public void testZoneDefinitionView() {
        ZoneDefinitionView zoneDefinitionView = new ZoneDefinitionView();
        zoneDefinitionView.setId("1");
        zoneDefinitionView.setRegion("region");
        zoneDefinitionView.setDescription("description");
        zoneDefinitionView.setStatus("status");
        zoneDefinitionView.setCreationTimestamp("time");
        zoneDefinitionView.setName("name");
        zoneDefinitionView.setSelfLink("link");
        zoneDefinitionView.setKind("kind");

        assertEquals("1", zoneDefinitionView.getId());
        assertEquals("time", zoneDefinitionView.getCreationTimestamp());
        assertEquals("link", zoneDefinitionView.getSelfLink());
        assertEquals("description", zoneDefinitionView.getDescription());
        assertEquals("kind", zoneDefinitionView.getKind());
        assertEquals("name", zoneDefinitionView.getName());
        assertEquals("region", zoneDefinitionView.getRegion());
        assertEquals("status", zoneDefinitionView.getStatus());
    }

}