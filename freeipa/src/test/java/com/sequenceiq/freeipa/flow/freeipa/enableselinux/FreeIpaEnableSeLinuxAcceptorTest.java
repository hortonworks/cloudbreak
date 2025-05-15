package com.sequenceiq.freeipa.flow.freeipa.enableselinux;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;

@ExtendWith(MockitoExtension.class)
public class FreeIpaEnableSeLinuxAcceptorTest {

    @InjectMocks
    private FreeIpaEnableSeLinuxAcceptor underTest;

    @Test
    void testSelector() {
        assertEquals(OperationType.MODIFY_SELINUX_MODE, underTest.selector());
    }
}