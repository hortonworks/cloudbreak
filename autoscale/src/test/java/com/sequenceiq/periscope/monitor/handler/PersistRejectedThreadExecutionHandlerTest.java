package com.sequenceiq.periscope.monitor.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.periscope.service.RejectedThreadService;

@ExtendWith(MockitoExtension.class)
class PersistRejectedThreadExecutionHandlerTest {

    @Mock
    private RejectedThreadService rejectedThreadService;

    @InjectMocks
    private PersistRejectedThreadExecutionHandler underTest;

    @Test
    void testConstructor() {
        underTest = new PersistRejectedThreadExecutionHandler();
    }

}
