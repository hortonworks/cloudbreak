package com.sequenceiq.freeipa.converter.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.StackStatus;

@ExtendWith(MockitoExtension.class)
class StackToAvailabiltyStatusConverterTest {

    @InjectMocks
    private StackToAvailabilityStatusConverter underTest;

    @Test
    void testConvertAvailable() {
        StackStatus status = new StackStatus();
        status.setDetailedStackStatus(DetailedStackStatus.AVAILABLE);
        assertEquals(AvailabilityStatus.AVAILABLE, underTest.convert(status));
    }

    @Test
    void testConvertUnavailable() {
        StackStatus status = new StackStatus();
        status.setDetailedStackStatus(DetailedStackStatus.UNREACHABLE);
        assertEquals(AvailabilityStatus.UNAVAILABLE, underTest.convert(status));
    }
}