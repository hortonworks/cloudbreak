package com.sequenceiq.freeipa.converter.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;

@ExtendWith(MockitoExtension.class)
class StackToAvailabiltyStatusConverterTest {

    @InjectMocks
    private StackToAvailabilityStatusConverter underTest;

    @Test
    void testConvertUnknown() {
        Stack stack = mock(Stack.class);
        assertEquals(AvailabilityStatus.UNKNOWN, underTest.convert(stack));
    }

    @Test
    void testConvertAvailable() {
        Stack stack = new Stack();
        StackStatus status = new StackStatus();
        status.setDetailedStackStatus(DetailedStackStatus.AVAILABLE);
        stack.setStackStatus(status);
        assertEquals(AvailabilityStatus.AVAILABLE, underTest.convert(stack));
    }

    @Test
    void testConvertUnavailable() {
        Stack stack = new Stack();
        StackStatus status = new StackStatus();
        status.setDetailedStackStatus(DetailedStackStatus.UNREACHABLE);
        stack.setStackStatus(status);
        assertEquals(AvailabilityStatus.UNAVAILABLE, underTest.convert(stack));
    }
}