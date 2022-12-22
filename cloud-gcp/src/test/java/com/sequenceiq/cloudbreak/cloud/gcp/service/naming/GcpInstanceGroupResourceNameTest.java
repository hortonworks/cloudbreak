package com.sequenceiq.cloudbreak.cloud.gcp.service.naming;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;

class GcpInstanceGroupResourceNameTest {
    private final GcpResourceNameService subject = new GcpResourceNameService();

    @Test
    void testLegacyName() {
        GcpInstanceGroupResourceName resourceName = subject.decodeInstanceGroupResourceNameFromString("test-master");
        Assertions.assertEquals("test", resourceName.getStackName());
        Assertions.assertEquals("master", resourceName.getGroupName());
    }

    @Test
    void testNewStackName() {
        GcpInstanceGroupResourceName resourceName = subject.decodeInstanceGroupResourceNameFromString("test-master-1");
        Assertions.assertEquals("test", resourceName.getStackName());
        Assertions.assertEquals("master", resourceName.getGroupName());
        Assertions.assertEquals("1", resourceName.getSuffix());
    }

    @Test
    void testUnsupported() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> subject.decodeInstanceGroupResourceNameFromString("master"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> subject.decodeInstanceGroupResourceNameFromString("test-master-11-new"));
    }
}