package com.sequenceiq.cloudbreak.cloud.gcp.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

public class GcpResourceNameServiceTest {

    private static final String MAX_RESOURCE_NAME_LENGTH = "63";

    private GcpResourceNameService subject;

    @BeforeEach
    public void setUp() {
        subject = new GcpResourceNameService();
        ReflectionTestUtils.setField(subject, "maxResourceNameLength", Integer.parseInt(MAX_RESOURCE_NAME_LENGTH));
    }

    @Test
    public void shouldGenerateNetworkResourceNameWhenStackNameProvided() {
        // GIVEN
        String[] parts = {"gcp Network"};

        // WHEN
        String networkResourceName = subject.resourceName(ResourceType.GCP_NETWORK, (Object[]) parts);

        // THEN
        Assertions.assertNotNull(networkResourceName, "The generated name must not be null!");
        Assertions.assertEquals(2L, networkResourceName.split("-").length, "The timestamp must be appended");
    }

    @Test
    public void shouldGenerateFirewallInternalResourceNameWhenStackNameProvided() {
        // GIVEN
        String stackName = "dummy #stack_name?";

        // WHEN
        String resourceName = subject.resourceName(ResourceType.GCP_FIREWALL_INTERNAL, stackName);

        // THEN
        Assertions.assertNotNull(resourceName, "The generated name must not be null!");
        Assertions.assertEquals(3L, resourceName.split("-").length, "The timestamp must be appended");
        Assertions.assertEquals("internal", resourceName.split("-")[1], "The resource name suffix is not the expected one!");
    }

    @Test
    public void shouldGenerateFirewallInResourceNameWhenStackNameProvided() {
        // GIVEN
        String stackName = "dummy #stack_name?";

        // WHEN
        String resourceName = subject.resourceName(ResourceType.GCP_FIREWALL_IN, stackName);

        // THEN
        Assertions.assertNotNull(resourceName, "The generated name must not be null!");
        Assertions.assertEquals(resourceName.split("-").length, 3L, "The timestamp must be appended");
        Assertions.assertEquals(resourceName.split("-")[1], "in", "The resource name suffix is not the expected one!");
    }

    @Test
    public void shouldShortenReservedIpResourceNameWhenLongResourceNameProvided() {
        //GIVEN
        Object[] parts = {"thisisaverylongtextwhichneedstobeshortenedbythespecificmethod",
                "thisisaverylonginstanceGroup", 8999};

        // WHEN
        String resourceName = subject.resourceName(ResourceType.GCP_RESERVED_IP, parts);

        // THEN
        Assertions.assertNotNull(resourceName, "The generated name must not be null!");
        Assertions.assertEquals(resourceName.split("-").length, 4L, "The timestamp must be appended!");
        Assertions.assertEquals(resourceName.split("-")[0], "thisisaverylongtextw", "The resource name suffix is not the excepted one!");
        Assertions.assertEquals(resourceName.split("-")[1], "t", "The instance group name is not the excepted one!");
        Assertions.assertEquals(resourceName.split("-")[2], "8999", "The private id is not the excepted one!");
        Assertions.assertTrue(resourceName.length() < Integer.parseInt(MAX_RESOURCE_NAME_LENGTH), "The resource name length is wrong");
    }

    @Test
    public void shouldGenerateGcpAttachedDiskResourceWhenPartsProvided() {
        // GIVEN
        Object[] parts = {"stack", "group", 3, 2};

        // WHEN
        String resourceName = subject.resourceName(ResourceType.GCP_ATTACHED_DISKSET, parts);

        // THEN
        Assertions.assertNotNull(resourceName, "The generated name must not be null!");
        Assertions.assertEquals(resourceName.split("-").length, 5L, "The timestamp must be appended");
        Assertions.assertTrue(resourceName.startsWith("stack-g-3-2"), "The resource name is not the expected one!");


    }

    @Test
    public void shouldHandleHealthCheckTypeFieldWithPort() {

        Object[] parts = {"stack", LoadBalancerType.PUBLIC, 8080};
        String resourceName = subject.resourceName(ResourceType.GCP_HEALTH_CHECK, parts);


        Assertions.assertNotNull(resourceName, "The generated name must not be null!");
        Assertions.assertTrue(resourceName.startsWith("stack-public-8080"), "The resource name is not the expected one!");
        Assertions.assertEquals(resourceName.split("-").length, 4L, "The timestamp must be appended");

    }

    @Test
    public void shouldShortenGcpInstanceNameWhenLongResourceNameProvided() {
        //GIVEN
        Object[] parts = {"thisisaverylongtextwhichneedstobeshortenedbythespecificmethod",
                "thisisaverylonginstanceGroup", 8999};

        //WHEN
        String resourceName = subject.resourceName(ResourceType.GCP_INSTANCE, parts);

        //THEN
        Assertions.assertNotNull(resourceName, "The generated name must not be null!");
        Assertions.assertEquals(resourceName.split("-").length, 4L, "The timestamp must be appended!");
        Assertions.assertEquals(resourceName.split("-")[0], "thisisaverylongtextw", "The resource name suffix is not the excepted one!");
        Assertions.assertEquals(resourceName.split("-")[1], "t", "The instance group name is not the excepted one!");
        Assertions.assertEquals(resourceName.split("-")[2], "8999", "The private is not the excepted one!");
        Assertions.assertTrue(resourceName.length() < Integer.parseInt(MAX_RESOURCE_NAME_LENGTH), "The resource name length is wrong");
    }

    @Test
    public void shouldShortenGcpInstanceGroupNameWhenLongResourceNameProvided() {
        //GIVEN
        Object[] parts = {"stackname", "thisisareallylonginstancenamewhichwillbeshortenedbythemethod", 8999};

        //WHEN

        String resourceName = subject.resourceName(ResourceType.GCP_INSTANCE, parts);

        //THEN
        Assertions.assertNotNull(resourceName, "The generated name must not be null!");
        Assertions.assertEquals(resourceName.split("-").length, 4L, "The timestamp must be appended!");
        Assertions.assertEquals(resourceName.split("-")[0], "stackname", "The resource name suffix is not the excepted one!");
        Assertions.assertEquals(resourceName.split("-")[1], "t", "The instance group name is not the excepted one!");
        Assertions.assertEquals(resourceName.split("-")[2], "8999", "The private is not the excepted one!");
        Assertions.assertTrue(resourceName.length() < Integer.parseInt(MAX_RESOURCE_NAME_LENGTH), "The resource name length is wrong");

    }

    @Test
    public void shouldGenerateGcpInstanceGroupResourceWehenPartsProvided() {
        // GIVEN
        Object[] parts = {"stack", "group", "1234"};

        // WHEN
        String resourceName = subject.resourceName(ResourceType.GCP_INSTANCE_GROUP, parts);

        // THEN
        Assertions.assertEquals(
                "stack-group-1234", resourceName, "The instance group resource name should include stack name, stack id and a group name");
        Assertions.assertEquals("group", subject.decodeInstanceGroupResourceNameFromString(resourceName).getGroupName());
        Assertions.assertEquals("stack", subject.decodeInstanceGroupResourceNameFromString(resourceName).getStackName());
    }
}