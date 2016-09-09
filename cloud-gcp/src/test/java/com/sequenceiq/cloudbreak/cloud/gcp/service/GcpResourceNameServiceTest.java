package com.sequenceiq.cloudbreak.cloud.gcp.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.service.ResourceNameService;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

public class GcpResourceNameServiceTest {

    private ResourceNameService subject;
    private final String maxResourceNameLength = "63";

    @Before
    public void setUp() throws Exception {
        subject = new GcpResourceNameService();
        ReflectionTestUtils.setField(subject, "maxResourceNameLength", Integer.parseInt(maxResourceNameLength));
    }

    @Test
    public void shouldGenerateNetworkResourceNameWhenStackNameProvided() throws Exception {
        // GIVEN
        String[] parts = new String[]{"gcp Network"};

        // WHEN
        String networkResourceName = subject.resourceName(ResourceType.GCP_NETWORK, (Object[]) parts);

        // THEN
        Assert.assertNotNull("The generated name must not be null!", networkResourceName);
        Assert.assertTrue("The timestamp must be appended", networkResourceName.split("-").length == 2);
    }

    @Test
    public void shouldGenerateFirewallInternalResourceNameWhenStackNameProvided() throws Exception {
        // GIVEN
        String stackName = "dummy #stack_name?";

        // WHEN
        String resourceName = subject.resourceName(ResourceType.GCP_FIREWALL_INTERNAL, stackName);

        // THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName);
        Assert.assertTrue("The timestamp must be appended", resourceName.split("-").length == 3);
        Assert.assertEquals("The resource name suffix is not the expected one!", "internal", resourceName.split("-")[1]);
    }

    @Test
    public void shouldGenerateFirewallInResourceNameWhenStackNameProvided() throws Exception {
        // GIVEN
        String stackName = "dummy #stack_name?";

        // WHEN
        String resourceName = subject.resourceName(ResourceType.GCP_FIREWALL_IN, stackName);

        // THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName);
        Assert.assertTrue("The timestamp must be appended", resourceName.split("-").length == 3);
        Assert.assertEquals("The resource name suffix is not the expected one!", "in", resourceName.split("-")[1]);
    }

    @Test
    public void shouldGenerateReservedIpResourceNameWhenStackNameProvided() throws Exception {
        // GIVEN
        String stackName = "dummy #stack_name?";

        // WHEN
        String resourceName = subject.resourceName(ResourceType.GCP_RESERVED_IP, stackName);

        // THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName);
        Assert.assertTrue("The timestamp must be appended", resourceName.split("-").length == 4);
        Assert.assertEquals("The resource name suffix is not the expected one!", "reserved", resourceName.split("-")[1]);
    }

    @Test
    public void shouldGenerateGcpAttachedDiskResourceWhenPartsProvided() throws Exception {
        // GIVEN
        Object[] parts = new Object[]{"stack", "group", 3, 2};

        // WHEN
        String resourceName = subject.resourceName(ResourceType.GCP_ATTACHED_DISK, parts);

        // THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName);
        Assert.assertTrue("The timestamp must be appended", resourceName.split("-").length == 5);
        Assert.assertTrue("The resource name is not the expected one!", resourceName.startsWith("stack-g-3-2"));


    }

    @Test
    public void shouldShortenGcpInstanceNameWhenLongResourceNameProvided() throws Exception {
        //GIVEN
        Object[] parts = new Object[]{"thisisaverylongtextwhichneedstobeshortenedbythespecificmethod",
                "thisisaverylonginstanceGroup", 8999};

        //WHEN
        String resourceName = subject.resourceName(ResourceType.GCP_INSTANCE, parts);

        //THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName);
        Assert.assertTrue("The timestamp must be appended!", resourceName.split("-").length == 4);
        Assert.assertEquals("The resource name suffix is not the excepted one!", "thisisaverylongtextw", resourceName.split("-")[0]);
        Assert.assertEquals("The instance group name is not the excepted one!", "t", resourceName.split("-")[1]);
        Assert.assertEquals("The private is not the excepted one!", "8999", resourceName.split("-")[2]);
        Assert.assertTrue("The resource name length is wrong", resourceName.length() < Integer.parseInt(maxResourceNameLength));
    }

    @Test
    public void shouldShortenGcpInstanceGroupNameWhenLongResourceNameProvided() throws Exception {
        //GIVEN
        Object[] parts = new Object[]{"stackname", "thisisareallylonginstancenamewhichwillbeshortenedbythemethod", 8999};

        //WHEN

        String resourceName = subject.resourceName(ResourceType.GCP_INSTANCE, parts);

        //THEN
        Assert.assertNotNull("The generated name must not be null!", resourceName);
        Assert.assertTrue("The timestamp must be appended!", resourceName.split("-").length == 4);
        Assert.assertEquals("The resource name suffix is not the excepted one!", "stackname", resourceName.split("-")[0]);
        Assert.assertEquals("The instance group name is not the excepted one!", "t", resourceName.split("-")[1]);
        Assert.assertEquals("The private is not the excepted one!", "8999", resourceName.split("-")[2]);
        Assert.assertTrue("The resource name length is wrong", resourceName.length() < Integer.parseInt(maxResourceNameLength));

    }
}