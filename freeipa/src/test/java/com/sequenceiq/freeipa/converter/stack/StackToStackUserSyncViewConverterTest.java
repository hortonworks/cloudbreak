package com.sequenceiq.freeipa.converter.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.entity.projection.StackUserSyncView;

public class StackToStackUserSyncViewConverterTest {

    private StackToStackUserSyncViewConverter converter;

    @BeforeEach
    void setUp() {
        converter = new StackToStackUserSyncViewConverter();
    }

    @Test
    void testConvert() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setResourceCrn("sampleResourceCrn");
        stack.setName("sampleStackName");
        stack.setEnvironmentCrn("sampleEnvironmentCrn");
        stack.setAccountId("sampleAccountId");
        stack.setCloudPlatform("sampleCloudPlatform");
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);

        StackUserSyncView result = converter.convert(stack);

        assertEquals(stack.getId(), result.id());
        assertEquals(stack.getResourceCrn(), result.resourceCrn());
        assertEquals(stack.getName(), result.name());
        assertEquals(stack.getEnvironmentCrn(), result.environmentCrn());
        assertEquals(stack.getAccountId(), result.accountId());
        assertEquals(stack.getCloudPlatform(), result.cloudPlatform());
        assertEquals(stack.getStackStatus().getStatus(), result.status());

    }
}
