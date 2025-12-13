package com.sequenceiq.cloudbreak.cloud.gcp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

public class ZoneDefinitionWrapperTest {

    @Test
    public void testZoneDefinitionWrapper() {
        ZoneDefinitionWrapper zoneDefinitionWrapper = new ZoneDefinitionWrapper();
        zoneDefinitionWrapper.setId("1");
        zoneDefinitionWrapper.setItems(new ArrayList<>());
        zoneDefinitionWrapper.setSelfLink("link");
        zoneDefinitionWrapper.setKind("kind");

        assertEquals("1", zoneDefinitionWrapper.getId());
        assertEquals(true, zoneDefinitionWrapper.getItems().isEmpty());
        assertEquals("link", zoneDefinitionWrapper.getSelfLink());
        assertEquals("kind", zoneDefinitionWrapper.getKind());

    }

}