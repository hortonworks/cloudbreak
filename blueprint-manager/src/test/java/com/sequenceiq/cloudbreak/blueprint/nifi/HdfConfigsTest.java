package com.sequenceiq.cloudbreak.blueprint.nifi;

import org.junit.Assert;
import org.junit.Test;

public class HdfConfigsTest {

    @Test
    public void testHdfConfigsIfGetSomePropertyThenShouldBeVisible() {
        HdfConfigs hdfConfigs = new HdfConfigs("entities", "entities", null);
        Assert.assertEquals("entities", hdfConfigs.getNodeEntities());
    }

}