package com.sequenceiq.cloudbreak.cloud.gcp.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.service.ResourceNameService;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

public class GcpResourceNameServiceTest {

    private static final String MAX_RESOURCE_NAME_LENGTH = "63";

    private ResourceNameService subject;

    @Before
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
        Assert.assertNotNull("The generated name must not be null!", networkResourceName);
        Assert.assertEquals("The timestamp must be appended", 2L, networkResourceName.split("-").length);
    }

    @Test
    public void shouldGenerateFirewallInternalResourceNameWhenStackNameProvided() {
        // GIVEN
        String stackName = "dummy #stack_name?";

        // WHEN
        String resourceName = subject.resourceName(ResourceType.GCP_FIREWALL_INTERNAL, stackName);

        // THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName);
        Assert.assertEquals("The timestamp must be appended", 3L, resourceName.split("-").length);
        Assert.assertEquals("The resource name suffix is not the expected one!", "internal", resourceName.split("-")[1]);
    }

    @Test
    public void shouldGenerateFirewallInResourceNameWhenStackNameProvided() {
        // GIVEN
        String stackName = "dummy #stack_name?";

        // WHEN
        String resourceName = subject.resourceName(ResourceType.GCP_FIREWALL_IN, stackName);

        // THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName);
        Assert.assertEquals("The timestamp must be appended", 3L, resourceName.split("-").length);
        Assert.assertEquals("The resource name suffix is not the expected one!", "in", resourceName.split("-")[1]);
    }

    @Test
    public void shouldShortenReservedIpResourceNameWhenLongResourceNameProvided() {
        //GIVEN
        Object[] parts = {"thisisaverylongtextwhichneedstobeshortenedbythespecificmethod",
                "thisisaverylonginstanceGroup", 8999};

        // WHEN
        String resourceName = subject.resourceName(ResourceType.GCP_RESERVED_IP, parts);

        // THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName);
        Assert.assertEquals("The timestamp must be appended!", 4L, resourceName.split("-").length);
        Assert.assertEquals("The resource name suffix is not the excepted one!", "thisisaverylongtextw", resourceName.split("-")[0]);
        Assert.assertEquals("The instance group name is not the excepted one!", "t", resourceName.split("-")[1]);
        Assert.assertEquals("The private id is not the excepted one!", "8999", resourceName.split("-")[2]);
        Assert.assertTrue("The resource name length is wrong", resourceName.length() < Integer.parseInt(MAX_RESOURCE_NAME_LENGTH));
    }

    @Test
    public void shouldGenerateGcpAttachedDiskResourceWhenPartsProvided() {
        // GIVEN
        Object[] parts = {"stack", "group", 3, 2};

        // WHEN
        String resourceName = subject.resourceName(ResourceType.GCP_ATTACHED_DISK, parts);

        // THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName);
        Assert.assertEquals("The timestamp must be appended", 5L, resourceName.split("-").length);
        Assert.assertTrue("The resource name is not the expected one!", resourceName.startsWith("stack-g-3-2"));


    }

    @Test
    public void shouldShortenGcpInstanceNameWhenLongResourceNameProvided() {
        //GIVEN
        Object[] parts = {"thisisaverylongtextwhichneedstobeshortenedbythespecificmethod",
                "thisisaverylonginstanceGroup", 8999};

        //WHEN
        String resourceName = subject.resourceName(ResourceType.GCP_INSTANCE, parts);

        //THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName);
        Assert.assertEquals("The timestamp must be appended!", 4L, resourceName.split("-").length);
        Assert.assertEquals("The resource name suffix is not the excepted one!", "thisisaverylongtextw", resourceName.split("-")[0]);
        Assert.assertEquals("The instance group name is not the excepted one!", "t", resourceName.split("-")[1]);
        Assert.assertEquals("The private is not the excepted one!", "8999", resourceName.split("-")[2]);
        Assert.assertTrue("The resource name length is wrong", resourceName.length() < Integer.parseInt(MAX_RESOURCE_NAME_LENGTH));
    }

    @Test
    public void shouldShortenGcpInstanceGroupNameWhenLongResourceNameProvided() {
        //GIVEN
        Object[] parts = {"stackname", "thisisareallylonginstancenamewhichwillbeshortenedbythemethod", 8999};

        //WHEN

        String resourceName = subject.resourceName(ResourceType.GCP_INSTANCE, parts);

        //THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName);
        Assert.assertEquals("The timestamp must be appended!", 4L, resourceName.split("-").length);
        Assert.assertEquals("The resource name suffix is not the excepted one!", "stackname", resourceName.split("-")[0]);
        Assert.assertEquals("The instance group name is not the excepted one!", "t", resourceName.split("-")[1]);
        Assert.assertEquals("The private is not the excepted one!", "8999", resourceName.split("-")[2]);
        Assert.assertTrue("The resource name length is wrong", resourceName.length() < Integer.parseInt(MAX_RESOURCE_NAME_LENGTH));

    }
}