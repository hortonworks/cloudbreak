package com.sequenceiq.cloudbreak.blueprint.nifi;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.template.model.HdfConfigs;

public class HdfConfigsTest {

    @Test
    public void testHdfConfigsIfGetSomePropertyThenShouldBeVisible() {
        HdfConfigs hdfConfigs = new HdfConfigs("entities", "entities", "entities", null);
        Assert.assertEquals("entities", hdfConfigs.getNodeEntities());
    }

}