package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class AwsResourceNameServiceTest {

    private static final Long STACK_ID = 123L;

    private static final String STACK_NAME = "stackName";

    private static final String GROUP_NAME = "groupName";

    private static final String SCHEME = "scheme";

    private static final Long PRIVATE_ID = 0L;

    private static final Integer PORT = 8080;

    @InjectMocks
    private AwsResourceNameService underTest;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", 70);
        ReflectionTestUtils.setField(underTest, "maxLoadBalancerResourceNameLength", 32);
    }

    @Test
    void testAttachedDisk() {
        String resourceName = underTest.attachedDisk(STACK_NAME, GROUP_NAME, PRIVATE_ID);
        assertTrue(resourceName.startsWith("stackname-groupname-0-"));
    }

    @Test
    void testNativeInstance() {
        String resourceName = underTest.nativeInstance(STACK_NAME, GROUP_NAME, STACK_ID, PRIVATE_ID);
        assertEquals("stackname-123-groupname-0", resourceName);
    }

    @Test
    void testRootDisk() {
        String resourceName = underTest.rootDisk(STACK_NAME, STACK_ID, GROUP_NAME, PRIVATE_ID);
        assertEquals("stackname-123-groupname-0", resourceName);
    }

    @Test
    void testSecurityGroup() {
        String resourceName = underTest.securityGroup(STACK_NAME, GROUP_NAME, STACK_ID);
        assertEquals("stackname-123-SecurityGroup-Groupname", resourceName);
    }

    @Test
    void testEip() {
        String resourceName = underTest.eip(STACK_NAME, GROUP_NAME, STACK_ID);
        assertEquals("stackname-123-EIPAllocationID-Groupname", resourceName);
    }

    @Test
    void testLoadBalancer() {
        String resourceName = underTest.loadBalancer(STACK_NAME, SCHEME);
        assertTrue(resourceName.matches("stacknam-LBscheme-\\d{14}"));
    }

    @Test
    void testLoadBalancerShorterThankSixCharacter() {
        String resourceName = underTest.loadBalancer("sdfsd", SCHEME);
        assertTrue(resourceName.matches("sdfsd-LBscheme-\\d{14}"));
    }

    @Test
    void testLoadBalancerTargetGroup() {
        String resourceName = underTest.loadBalancerTargetGroup(STACK_NAME, SCHEME, PORT);
        assertTrue(resourceName.matches("stac-TG8080scheme-\\d{14}"));
    }

    @Test
    void testLoadBalancerTargetGroupShorterThankSixCharacter() {
        String resourceName = underTest.loadBalancerTargetGroup("sdfsd", SCHEME, PORT);
        assertTrue(resourceName.matches("sdfs-TG8080scheme-\\d{14}"));
    }

    @Test
    void testCloudWatch() {
        String resourceName = underTest.cloudWatch(STACK_NAME, STACK_ID, GROUP_NAME, PRIVATE_ID);
        assertEquals("stackname-123-CloudWatch-Groupname-0", resourceName);
    }

    @Test
    void testAdjustPartLengthWhenNoShortenNeed() {
        String actual = underTest.adjustPartLength("part1", 10);
        assertEquals("part1", actual);
    }

    @Test
    void testAdjustPartLengthWhenMaxLengthSameAsThePartLength() {
        String actual = underTest.adjustPartLength("part1", 5);
        assertEquals("part1", actual);
    }

    @Test
    void testAdjustPartLengthWhenShortenNeed() {
        String actual = underTest.adjustPartLength("part1", 2);
        assertEquals("pa", actual);
    }

    @Test
    void testAdjustPartLengthWhenMaxLength1() {
        String actual = underTest.adjustPartLength("part1", 1);
        assertEquals("p", actual);
    }

    @Test
    void testAdjustPartLengthWhenMaxLengthSameAsThePartLengthAndMaxLegth1() {
        String actual = underTest.adjustPartLength("p", 1);
        assertEquals("p", actual);
    }
}
