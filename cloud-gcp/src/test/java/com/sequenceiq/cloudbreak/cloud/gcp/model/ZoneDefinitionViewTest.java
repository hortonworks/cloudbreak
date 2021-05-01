package com.sequenceiq.cloudbreak.cloud.gcp.model;

import org.junit.Assert;
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

        Assert.assertEquals("1", zoneDefinitionView.getId());
        Assert.assertEquals("time", zoneDefinitionView.getCreationTimestamp());
        Assert.assertEquals("link", zoneDefinitionView.getSelfLink());
        Assert.assertEquals("description", zoneDefinitionView.getDescription());
        Assert.assertEquals("kind", zoneDefinitionView.getKind());
        Assert.assertEquals("name", zoneDefinitionView.getName());
        Assert.assertEquals("region", zoneDefinitionView.getRegion());
        Assert.assertEquals("status", zoneDefinitionView.getStatus());
    }

}