package com.sequenceiq.cloudbreak.templateprocessor.nifi;

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class HdfConfigsTest {

    @Test
    public void testHdfConfigsIfGetSomePropertyThenShouldBeVisible() {
        HdfConfigs hdfConfigs = new HdfConfigs("entities", Optional.empty());
        Assert.assertEquals("entities", hdfConfigs.getNodeEntities());
    }

}