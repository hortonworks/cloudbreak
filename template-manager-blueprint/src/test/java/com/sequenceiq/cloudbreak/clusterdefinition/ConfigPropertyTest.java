package com.sequenceiq.cloudbreak.clusterdefinition;

import org.junit.Assert;
import org.junit.Test;

public class ConfigPropertyTest {

    @Test
    public void testConfigPropertyIfInitialized() {
        ConfigProperty configProperty = new ConfigProperty("namenode", "/fs1", "hadoopfs");

        Assert.assertEquals("/fs1", configProperty.getDirectory());
        Assert.assertEquals("hadoopfs", configProperty.getPrefix());
        Assert.assertEquals("namenode", configProperty.getName());
    }

}