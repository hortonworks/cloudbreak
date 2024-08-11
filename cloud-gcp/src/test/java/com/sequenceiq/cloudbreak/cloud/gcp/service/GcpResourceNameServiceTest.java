package com.sequenceiq.cloudbreak.cloud.gcp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.common.api.type.LoadBalancerType;

@ExtendWith(MockitoExtension.class)
public class GcpResourceNameServiceTest {

    private static final String MAX_RESOURCE_NAME_LENGTH = "63";

    private static final Long STACK_ID = 123L;

    private static final String STACK_NAME = "stackName";

    private static final String GROUP_NAME = "groupName";

    private static final LoadBalancerType LOAD_BALACNER_TYPE = LoadBalancerType.GATEWAY_PRIVATE;

    private static final Long PRIVATE_ID = 0L;

    private static final Integer PORT = 8080;

    private static final int COUNT = 1;

    @InjectMocks
    private GcpResourceNameService underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", Integer.parseInt(MAX_RESOURCE_NAME_LENGTH));
    }

    @Test
    void testAttachedDisk() {
        String resourceName = underTest.attachedDisk(STACK_NAME, GROUP_NAME, PRIVATE_ID, COUNT);
        assertTrue(resourceName.matches("stackname-g-0-1-\\d{13}"));
    }

    @Test
    void testDeploymentTemplate() {
        String resourceName = underTest.deploymentTemplate(STACK_NAME, STACK_ID);
        assertTrue(resourceName.startsWith("stackname-"));
    }

    @Test
    void testInstance() {
        String resourceName = underTest.instance(STACK_NAME, GROUP_NAME, PRIVATE_ID);
        assertTrue(resourceName.matches("stackname-g-0-\\d{13}"));
    }

    @Test
    void testFirewallIn() {
        String resourceName = underTest.firewallIn(STACK_NAME);
        assertTrue(resourceName.matches("stackname-in-\\d{13}"));
    }

    @Test
    void testFirewallInternal() {
        String resourceName = underTest.firewallInternal(STACK_NAME);
        assertTrue(resourceName.matches("stackname-internal-\\d{13}"));
    }

    @Test
    void testNetwork() {
        String resourceName = underTest.network(STACK_NAME);
        assertTrue(resourceName.matches("stackname-\\d{13}"));
    }

    @Test
    void testSubnet() {
        String resourceName = underTest.subnet(STACK_NAME);
        assertTrue(resourceName.matches("stackname-\\d{13}"));
    }

    @Test
    void testGroup() {
        String resourceName = underTest.group(STACK_NAME, GROUP_NAME, STACK_ID, "");
        assertEquals("stackname-groupname-123", resourceName);
    }

    @Test
    void testGroupWithAz() {
        String resourceName = underTest.group(STACK_NAME, GROUP_NAME, STACK_ID, "us-west2-c");
        assertEquals("stackname-groupname-123-c", resourceName);
    }

    @Test
    void testLoadBalancerWithPortWhenLoadBalancerTypeNotContainsUnderscore() {
        String resourceName = underTest.loadBalancerWithPort(STACK_NAME, LoadBalancerType.PUBLIC, PORT);
        assertTrue(resourceName.matches("stackname-public-8080-\\d{13}"));
    }

    @Test
    void testLoadBalancerWithPortWhenLoadBalancerTypeContainsUnderscore() {
        String resourceName = underTest.loadBalancerWithPort(STACK_NAME, LOAD_BALACNER_TYPE, PORT);
        assertTrue(resourceName.matches("stackname-gatewayprivate-8080-\\d{13}"));
    }

    @Test
    void shouldGenerateNetworkResourceNameWhenStackNameProvided() {
        String stackName = "gcp Network";

        String networkResourceName = underTest.network(stackName);

        assertNotNull(networkResourceName, "The generated name must not be null!");
        assertEquals(2L, networkResourceName.split("-").length, "The timestamp must be appended");
    }

    @Test
    void shouldGenerateFirewallInternalResourceNameWhenStackNameProvided() {
        String stackName = "dummy #stack_name?";

        String resourceName = underTest.firewallInternal(stackName);

        assertNotNull(resourceName, "The generated name must not be null!");
        assertEquals(3L, resourceName.split("-").length, "The timestamp must be appended");
        assertEquals("internal", resourceName.split("-")[1], "The resource name suffix is not the expected one!");
    }

    @Test
    void shouldGenerateFirewallInResourceNameWhenStackNameProvided() {
        String stackName = "dummy #stack_name?";

        String resourceName = underTest.firewallIn(stackName);

        assertNotNull(resourceName, "The generated name must not be null!");
        assertEquals(resourceName.split("-").length, 3L, "The timestamp must be appended");
        assertEquals(resourceName.split("-")[1], "in", "The resource name suffix is not the expected one!");
    }

    @Test
    void shouldShortenReservedIpResourceNameWhenLongResourceNameProvided() {
        String resourceName = underTest.instance("thisisaverylongtextwhichneedstobeshortenedbythespecificmethod",
                "thisisaverylonginstanceGroup", 8999L);

        assertNotNull(resourceName, "The generated name must not be null!");
        assertEquals(resourceName.split("-").length, 4L, "The timestamp must be appended!");
        assertEquals(resourceName.split("-")[0], "thisisaverylongtextw", "The resource name suffix is not the excepted one!");
        assertEquals(resourceName.split("-")[1], "t", "The instance group name is not the excepted one!");
        assertEquals(resourceName.split("-")[2], "8999", "The private id is not the excepted one!");
        assertTrue(resourceName.length() < Integer.parseInt(MAX_RESOURCE_NAME_LENGTH), "The resource name length is wrong");
    }

    @Test
    void shouldGenerateGcpAttachedDiskResourceWhenPartsProvided() {
        Object[] parts = {"stack", "group", 3, 2};

        String resourceName = underTest.attachedDisk("stack", "group", 3L, 2);

        assertNotNull(resourceName, "The generated name must not be null!");
        assertEquals(resourceName.split("-").length, 5L, "The timestamp must be appended");
        assertTrue(resourceName.startsWith("stack-g-3-2"), "The resource name is not the expected one!");


    }

    @Test
    void shouldHandleHealthCheckTypeFieldWithPort() {
        String resourceName = underTest.loadBalancerWithPort("stack", LoadBalancerType.PUBLIC, 8080);

        assertNotNull(resourceName, "The generated name must not be null!");
        assertTrue(resourceName.startsWith("stack-public-8080"), "The resource name is not the expected one!");
        assertEquals(resourceName.split("-").length, 4L, "The timestamp must be appended");

    }

    @Test
    void shouldShortenGcpInstanceNameWhenLongResourceNameProvided() {
        String resourceName = underTest.instance("thisisaverylongtextwhichneedstobeshortenedbythespecificmethod",
                "thisisaverylonginstanceGroup", 8999L);

        assertNotNull(resourceName, "The generated name must not be null!");
        assertEquals(resourceName.split("-").length, 4L, "The timestamp must be appended!");
        assertEquals(resourceName.split("-")[0], "thisisaverylongtextw", "The resource name suffix is not the excepted one!");
        assertEquals(resourceName.split("-")[1], "t", "The instance group name is not the excepted one!");
        assertEquals(resourceName.split("-")[2], "8999", "The private is not the excepted one!");
        assertTrue(resourceName.length() < Integer.parseInt(MAX_RESOURCE_NAME_LENGTH), "The resource name length is wrong");
    }

    @Test
    void shouldShortenGcpInstanceGroupNameWhenLongResourceNameProvided() {
        String resourceName = underTest.instance("stackname", "thisisareallylonginstancenamewhichwillbeshortenedbythemethod", 8999L);

        assertNotNull(resourceName, "The generated name must not be null!");
        assertEquals(resourceName.split("-").length, 4L, "The timestamp must be appended!");
        assertEquals(resourceName.split("-")[0], "stackname", "The resource name suffix is not the excepted one!");
        assertEquals(resourceName.split("-")[1], "t", "The instance group name is not the excepted one!");
        assertEquals(resourceName.split("-")[2], "8999", "The private is not the excepted one!");
        assertTrue(resourceName.length() < Integer.parseInt(MAX_RESOURCE_NAME_LENGTH), "The resource name length is wrong");

    }

    @Test
    void shouldGenerateGcpInstanceGroupResourceWehenPartsProvided() {
        String resourceName = underTest.group("stack", "group", 1234L, "");

        assertEquals(
                "stack-group-1234", resourceName, "The instance group resource name should include stack name, stack id and a group name");
        assertEquals("group", underTest.decodeInstanceGroupResourceNameFromString(resourceName).getGroupName());
        assertEquals("stack", underTest.decodeInstanceGroupResourceNameFromString(resourceName).getStackName());
    }
}
