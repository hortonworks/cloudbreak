package com.sequenceiq.cloudbreak.cloud.gcp.model;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class ZoneDefinitionWrapperTest {

    @Test
    public void testZoneDefinitionWrapper() {
        ZoneDefinitionWrapper zoneDefinitionWrapper = new ZoneDefinitionWrapper();
        zoneDefinitionWrapper.setId("1");
        zoneDefinitionWrapper.setItems(new ArrayList<>());
        zoneDefinitionWrapper.setSelfLink("link");
        zoneDefinitionWrapper.setKind("kind");

        Assert.assertEquals("1", zoneDefinitionWrapper.getId());
        Assert.assertEquals(true, zoneDefinitionWrapper.getItems().isEmpty());
        Assert.assertEquals("link", zoneDefinitionWrapper.getSelfLink());
        Assert.assertEquals("kind", zoneDefinitionWrapper.getKind());

    }

}