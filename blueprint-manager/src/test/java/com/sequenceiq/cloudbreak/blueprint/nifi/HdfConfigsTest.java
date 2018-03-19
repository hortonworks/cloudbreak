package com.sequenceiq.cloudbreak.blueprint.nifi;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

public class HdfConfigsTest {

    @Test
    public void testHdfConfigsIfGetSomePropertyThenShouldBeVisible() {
        HdfConfigs hdfConfigs = new HdfConfigs("entities", Optional.empty());
        Assert.assertEquals("entities", hdfConfigs.getNodeEntities());
    }

}