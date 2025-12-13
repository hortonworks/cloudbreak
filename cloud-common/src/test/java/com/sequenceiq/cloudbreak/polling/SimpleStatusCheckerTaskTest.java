package com.sequenceiq.cloudbreak.polling;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

class SimpleStatusCheckerTaskTest {

    private SimpleStatusCheckerTask<Object> underTest;

    @BeforeEach
    void setup() {
        underTest = new SimpleStatusCheckerTask<>() {
            @Override
            public boolean checkStatus(Object o) {
                return false;
            }

            @Override
            public void handleTimeout(Object o) {

            }

            @Override
            public String successMessage(Object o) {
                return null;
            }

            @Override
            public boolean exitPolling(Object o) {
                return false;
            }
        };
    }

    @Test
    void testHandleException() {
        assertThrows(CloudbreakServiceException.class, () -> underTest.handleException(new RuntimeException()));
    }

    @Test
    void testWeThrowChildException() {
        // Child of SimpleStatusCheckerTaskTestException should be thrown and we shall not wrap it
        // since SimpleStatusCheckerTaskTestException is a child of CloudbreakServiceException
        assertThrows(SimpleStatusCheckerTaskTestException.class, () -> underTest.handleException(new SimpleStatusCheckerTaskTestException()));
    }

    static class SimpleStatusCheckerTaskTestException extends CloudbreakServiceException {

        SimpleStatusCheckerTaskTestException() {
            super("message");
        }

    }
}