package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class AwsResourceNameServiceTest {

    private static final Long STACK_ID = 123L;

    private static final long PRIVATE_ID = 0L;

    @InjectMocks
    private AwsResourceNameService underTest;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", 70);
    }

    @Test
    public void testResourceNamweWhenAwsInstance() {
        String actual = underTest.resourceName(ResourceType.AWS_INSTANCE, "stackName", "groupName", STACK_ID, PRIVATE_ID);
        Assertions.assertEquals("stackname-123-groupname-0", actual);
    }

    @Test
    public void testAdjustPartLengthWhenNoShortenNeed() {
        String actual = underTest.adjustPartLength("part1", 10);
        Assertions.assertEquals("part1", actual);
    }

    @Test
    public void testAdjustPartLengthWhenMaxLengthSameAsThePartLength() {
        String actual = underTest.adjustPartLength("part1", 5);
        Assertions.assertEquals("part1", actual);
    }

    @Test
    public void testAdjustPartLengthWhenShortenNeed() {
        String actual = underTest.adjustPartLength("part1", 2);
        Assertions.assertEquals("pa", actual);
    }

    @Test
    public void testAdjustPartLengthWhenMaxLength1() {
        String actual = underTest.adjustPartLength("part1", 1);
        Assertions.assertEquals("p", actual);
    }

    @Test
    public void testAdjustPartLengthWhenMaxLengthSameAsThePartLengthAndMaxLegth1() {
        String actual = underTest.adjustPartLength("p", 1);
        Assertions.assertEquals("p", actual);
    }
}
