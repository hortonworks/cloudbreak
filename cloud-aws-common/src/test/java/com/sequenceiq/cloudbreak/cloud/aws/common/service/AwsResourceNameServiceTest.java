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
class AwsResourceNameServiceTest {

    private static final Long STACK_ID = 123L;

    private static final String STACK_NAME = "stackName";

    private static final String STACK_NAME_LONG = "stackNameLong";

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
        String resourceName = underTest.loadBalancer(STACK_NAME_LONG, SCHEME);
        assertTrue(resourceName.matches("stacknam-LBscheme-\\d{13}"));
    }

    @Test
    void testLoadBalancerBackwardCompatibleWithNewHash() {
        //satori-LBGwayPriv-20240521151654
        String resourceName = underTest.loadBalancer("satori-dev", "GwayPriv");
        assertTrue(resourceName.matches("satori-LBGwayPriv-\\d{13}"));
    }

    @Test
    void testLoadBalancerShorterThanSevenCharacter() {
        String resourceName = underTest.loadBalancer("sdfsda", SCHEME);
        assertTrue(resourceName.matches("sdfsda-LBscheme-\\d{13}"));
    }

    @Test
    void testLoadBalancerTargetGroup() {
        String resourceName = underTest.loadBalancerTargetGroup(STACK_NAME, SCHEME, PORT);
        assertTrue(resourceName.matches("stac-TG8080scheme-\\d{13}"));
    }

    @Test
    void testLoadBalancerTargetGroupBackwardCompatibleWithNewHash() {
        String resourceName = underTest.loadBalancerTargetGroup("sator-dev", "GwayPriv", 443);
        assertTrue(resourceName.matches("sat-TG443GwayPriv-\\d{13}"));
    }

    @Test
    void testLoadBalancerTargetGroupShorterThanSevenCharacter() {
        String resourceName = underTest.loadBalancerTargetGroup("sdfsda", SCHEME, PORT);
        assertTrue(resourceName.matches("sdfs-TG8080scheme-\\d{13}"));
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
