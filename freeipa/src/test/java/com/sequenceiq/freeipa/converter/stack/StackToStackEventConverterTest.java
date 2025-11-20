package com.sequenceiq.freeipa.converter.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.dto.StackEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;

class StackToStackEventConverterTest {

    private static final Long STACK_ID = 123L;

    private static final String STACK_NAME = "test-stack";

    private static final String RESOURCE_CRN = "crn:cdp:freeipa:us-west-1:tenant:freeipa:stack-id";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:tenant:environment:env-id";

    private static final String ACCOUNT_ID = "account-id";

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String REGION = "us-west-1";

    private static final String AVAILABILITY_ZONE = "us-west-1a";

    private static final Status STATUS = Status.AVAILABLE;

    private StackToStackEventConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new StackToStackEventConverter();
    }

    @Test
    void convertShouldMapAllFieldsCorrectly() {
        Stack stack = createStack();

        StackEvent result = underTest.convert(stack);

        assertNotNull(result);
        assertEquals(STACK_ID, result.getId());
        assertEquals(STACK_NAME, result.getName());
        assertEquals(RESOURCE_CRN, result.getResourceCrn());
        assertEquals(ENVIRONMENT_CRN, result.getEnvironmentCrn());
        assertEquals(CLOUD_PLATFORM, result.getCloudPlatform());
        assertEquals(REGION, result.getRegion());
        assertEquals(AVAILABILITY_ZONE, result.getAvailabilityZone());
        assertEquals(ACCOUNT_ID, result.getAccountId());
        assertEquals(STATUS, result.getStatus());
    }

    @Test
    void convertShouldHandleNullStackStatus() {
        Stack stack = createStack();
        stack.setStackStatus(null);

        StackEvent result = underTest.convert(stack);

        assertNotNull(result);
        assertEquals(STACK_ID, result.getId());
        assertEquals(STACK_NAME, result.getName());
        assertEquals(RESOURCE_CRN, result.getResourceCrn());
        assertEquals(ENVIRONMENT_CRN, result.getEnvironmentCrn());
        assertEquals(CLOUD_PLATFORM, result.getCloudPlatform());
        assertEquals(REGION, result.getRegion());
        assertEquals(AVAILABILITY_ZONE, result.getAvailabilityZone());
        assertEquals(ACCOUNT_ID, result.getAccountId());
        assertNull(result.getStatus());
    }

    @Test
    void convertShouldHandleNullFields() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);

        StackEvent result = underTest.convert(stack);

        assertNotNull(result);
        assertEquals(STACK_ID, result.getId());
        assertNull(result.getName());
        assertNull(result.getResourceCrn());
        assertNull(result.getEnvironmentCrn());
        assertNull(result.getCloudPlatform());
        assertNull(result.getRegion());
        assertNull(result.getAvailabilityZone());
        assertNull(result.getAccountId());
        assertNull(result.getStatus());
    }

    @Test
    void convertShouldHandleStackStatusWithDifferentStatuses() {
        Stack stack = createStack();
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.CREATE_IN_PROGRESS);
        stack.setStackStatus(stackStatus);

        StackEvent result = underTest.convert(stack);

        assertNotNull(result);
        assertEquals(Status.CREATE_IN_PROGRESS, result.getStatus());
    }

    @Test
    void convertShouldHandleEmptyStrings() {
        Stack stack = createStack();
        stack.setName("");
        stack.setResourceCrn("");
        stack.setEnvironmentCrn("");
        stack.setAccountId("");
        stack.setCloudPlatform("");
        stack.setRegion("");
        stack.setAvailabilityZone("");

        StackEvent result = underTest.convert(stack);

        assertNotNull(result);
        assertEquals("", result.getName());
        assertEquals("", result.getResourceCrn());
        assertEquals("", result.getEnvironmentCrn());
        assertEquals("", result.getAccountId());
        assertEquals("", result.getCloudPlatform());
        assertEquals("", result.getRegion());
        assertEquals("", result.getAvailabilityZone());
    }

    @Test
    void convertShouldCreateNewStackEventInstance() {
        Stack stack = createStack();

        StackEvent result1 = underTest.convert(stack);
        StackEvent result2 = underTest.convert(stack);

        assertNotNull(result1);
        assertNotNull(result2);
        assert result1 != result2;
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME);
        stack.setResourceCrn(RESOURCE_CRN);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setAccountId(ACCOUNT_ID);
        stack.setCloudPlatform(CLOUD_PLATFORM);
        stack.setRegion(REGION);
        stack.setAvailabilityZone(AVAILABILITY_ZONE);

        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(STATUS);
        stack.setStackStatus(stackStatus);

        return stack;
    }
}