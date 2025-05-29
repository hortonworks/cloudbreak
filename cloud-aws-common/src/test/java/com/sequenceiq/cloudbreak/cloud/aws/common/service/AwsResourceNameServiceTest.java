package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;

@ExtendWith(MockitoExtension.class)
class AwsResourceNameServiceTest {

    private static final Long STACK_ID = 123L;

    private static final String STACK_NAME = "stackName";

    private static final String STACK_NAME_LONG = "stackNameLong";

    private static final String GROUP_NAME = "groupName";

    private static final String SCHEME = "scheme";

    private static final Long PRIVATE_ID = 0L;

    private static final Integer PORT = 8080;

    private static final CloudContext CLOUD_CONTEXT = CloudContext.Builder.builder()
            .withCrn("crn:cdp:freeipa:us-west-1:cloudera:freeipa:e5ad92a0-148e-4681-8afa-030c8b4912a2").build();

    private static final String CRN_PART = "e5ad92a0";

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
        String resourceName = underTest.loadBalancer(STACK_NAME_LONG, SCHEME, CLOUD_CONTEXT);
        assertTrue(resourceName.matches("stacknamel-scheme-" + CRN_PART + "-[A-Za-z0-9]{4}"));
    }

    @Test
    void testLoadBalancerBackwardCompatibleWithNewHash() {
        String resourceName = underTest.loadBalancer("satori-dev", "GwayPriv", CLOUD_CONTEXT);
        assertTrue(resourceName.matches("satorid-GwayPriv-" + CRN_PART + "-[A-Za-z0-9]{4}"));
    }

    @Test
    void testLoadBalancerShorterThanSevenCharacter() {
        String resourceName = underTest.loadBalancer("sdfsda", SCHEME, CLOUD_CONTEXT);
        assertTrue(resourceName.matches("sdfsda-scheme-" + CRN_PART + "-[A-Za-z0-9]{4}"));
    }

    @Test
    void testLoadBalancerTargetGroup() {
        String resourceName = underTest.loadBalancerTargetGroup(STACK_NAME, SCHEME, PORT, CLOUD_CONTEXT);
        assertTrue(resourceName.matches("stac-TG8080scheme-" + CRN_PART + "-[A-Za-z0-9]{4}"));
    }

    @Test
    void testLoadBalancerTargetGroupBackwardCompatibleWithNewHash() {
        String resourceName = underTest.loadBalancerTargetGroup("sator-dev", "GwayPriv", 443, CLOUD_CONTEXT);
        assertTrue(resourceName.matches("sat-TG443GwayPriv-" + CRN_PART + "-[A-Za-z0-9]{4}"));
    }

    @Test
    void testLoadBalancerTargetGroupShorterThanSevenCharacter() {
        String resourceName = underTest.loadBalancerTargetGroup("sdfsda", SCHEME, PORT, CLOUD_CONTEXT);
        assertTrue(resourceName.matches("sdfs-TG8080scheme-" + CRN_PART + "-[A-Za-z0-9]{4}"));
    }

    @Disabled
    @Test
    void testLoadBalancerTargetGroupMultipleGenerationTestForCollision() {
        String resourceName = underTest.loadBalancerTargetGroup("stackname-1", SCHEME, PORT, CLOUD_CONTEXT);
        String otherResourceName = underTest.loadBalancerTargetGroup("stackname-2", SCHEME, PORT, CLOUD_CONTEXT);
        String anOtherResourceName = underTest.loadBalancerTargetGroup("stackname-3", SCHEME, PORT, CLOUD_CONTEXT);
        assertTrue(resourceName.matches("stac-TG8080scheme-\\d{14}"));
        assertTrue(otherResourceName.matches("stac-TG8080scheme-\\d{14}"));
        assertNotEquals(resourceName, otherResourceName);
        assertNotEquals(resourceName, anOtherResourceName);
        assertNotEquals(otherResourceName, anOtherResourceName);
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
