package com.sequenceiq.cloudbreak.template.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConfigPropertyTest {

    @Test
    void testConfigPropertyIfInitialized() {
        ConfigProperty configProperty = new ConfigProperty("namenode", "/fs1", "hadoopfs");

        assertEquals("/fs1", configProperty.getDirectory());
        assertEquals("hadoopfs", configProperty.getPrefix());
        assertEquals("namenode", configProperty.getName());
    }

}