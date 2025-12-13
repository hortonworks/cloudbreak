package com.sequenceiq.cloudbreak.cloud.gcp.service.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;

class GcpInstanceGroupResourceNameTest {
    private final GcpResourceNameService subject = new GcpResourceNameService();

    @Test
    void testLegacyName() {
        GcpInstanceGroupResourceName resourceName = subject.decodeInstanceGroupResourceNameFromString("test-master");
        assertEquals("test", resourceName.getStackName());
        assertEquals("master", resourceName.getGroupName());
    }

    @Test
    void testNewStackName() {
        GcpInstanceGroupResourceName resourceName = subject.decodeInstanceGroupResourceNameFromString("test-master-1");
        assertEquals("test", resourceName.getStackName());
        assertEquals("master", resourceName.getGroupName());
        assertEquals("1", resourceName.getSuffix());
    }

    @Test
    void testUnsupported() {
        assertThrows(IllegalArgumentException.class, () -> subject.decodeInstanceGroupResourceNameFromString("master"));
        assertThrows(IllegalArgumentException.class, () -> subject.decodeInstanceGroupResourceNameFromString("test-master-11-new"));
    }
}