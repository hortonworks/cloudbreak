package com.sequenceiq.environment.experience;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class ResponseReaderUtilityTest {

    private static final String BASE_MESSAGE = "some base message";

    @Mock
    private Logger mockLogger;

    @Test
    @DisplayName("Test when writing value from source doesn't cause any error then no logger warn should be called.")
    void testNoException() {
        ResponseReaderUtility.logInputResponseContentIfPossible(mockLogger, new Object(), BASE_MESSAGE);

        verify(mockLogger, times(1)).info(eq(BASE_MESSAGE), any(Object.class));
        verifyNoMoreInteractions(mockLogger);
    }

}