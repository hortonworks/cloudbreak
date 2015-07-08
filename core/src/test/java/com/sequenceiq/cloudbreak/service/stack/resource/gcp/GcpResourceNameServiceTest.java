package com.sequenceiq.cloudbreak.service.stack.resource.gcp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceNameService;

public class GcpResourceNameServiceTest {

    private ResourceNameService subject;

    @Before
    public void setUp() throws Exception {
        subject = new GcpResourceNameService();
    }

    @Test
    public void shouldGenerateNetworkResourceNameWhenStackNameProvided() throws Exception {
        // GIVEN
        String[] parts = new String[]{"gcp Network"};

        // WHEN
        String networkResourceName = subject.resourceName(ResourceType.GCP_NETWORK, parts);

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
        Assert.assertTrue("The resource name is not the expected one!", resourceName.startsWith("group-3-stack-2"));


    }
}