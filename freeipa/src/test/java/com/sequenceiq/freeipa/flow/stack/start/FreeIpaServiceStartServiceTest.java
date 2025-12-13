package com.sequenceiq.freeipa.flow.stack.start;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
public class FreeIpaServiceStartServiceTest {

    @Mock
    AttemptMakerFactory attemptMakerFactory;

    @Mock
    OneFreeIpaReachableAttempt oneFreeIpaReachableAttempt;

    @InjectMocks
    FreeIpaServiceStartService freeIpaServiceStartServiceUnderTest;

    private Stack stack;

    @BeforeEach
    void setup() {
        this.stack = new Stack();
        when(attemptMakerFactory.create(any(), any(), anyInt())).thenReturn(oneFreeIpaReachableAttempt);
        ReflectionTestUtils.setField(freeIpaServiceStartServiceUnderTest, "attempt", 2);
        ReflectionTestUtils.setField(freeIpaServiceStartServiceUnderTest, "sleepingTime", 1);
        ReflectionTestUtils.setField(freeIpaServiceStartServiceUnderTest, "consecutiveSuccess", 1);
    }

    @Test
    public void testPollFreeIpaHealthSucceed() throws Exception {
        doReturn(AttemptResults.justFinish()).when(oneFreeIpaReachableAttempt).process();
        assertDoesNotThrow(() -> freeIpaServiceStartServiceUnderTest.pollFreeIpaHealth(stack));
        verify(oneFreeIpaReachableAttempt, times(1)).process();
    }

    @Test
    public void testPollFreeIpaHealthFails() throws Exception {
        doReturn(AttemptResults.breakFor("fail")).when(oneFreeIpaReachableAttempt).process();
        assertThrows(OperationException.class, () -> freeIpaServiceStartServiceUnderTest.pollFreeIpaHealth(stack));
        verify(oneFreeIpaReachableAttempt, times(1)).process();
    }

    @Test
    public void testPollFreeIpaHealthContinue() throws Exception {
        doReturn(AttemptResults.justContinue()).when(oneFreeIpaReachableAttempt).process();
        assertThrows(OperationException.class, () -> freeIpaServiceStartServiceUnderTest.pollFreeIpaHealth(stack));
        verify(oneFreeIpaReachableAttempt, times(2)).process();
    }
}