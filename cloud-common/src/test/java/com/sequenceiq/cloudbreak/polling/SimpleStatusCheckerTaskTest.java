package com.sequenceiq.cloudbreak.polling;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class SimpleStatusCheckerTaskTest {

    private SimpleStatusCheckerTask<Object> underTest;

    @Before
    public void setup() {
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

    @Test(expected = CloudbreakServiceException.class)
    public void testHandleException() {
        underTest.handleException(new RuntimeException());
        Assert.fail("This shall not pass, since handleException is expected to throw an Exception");
    }

    @Test(expected = SimpleStatusCheckerTaskTestException.class)
    public void testWeThrowChildException() {
        // Child of SimpleStatusCheckerTaskTestException should be thrown and we shall not wrap it
        // since SimpleStatusCheckerTaskTestException is a child of CloudbreakServiceException
        underTest.handleException(new SimpleStatusCheckerTaskTestException());
        Assert.fail("This shall not pass, since handleException is expected to throw an Exception");
    }

    static class SimpleStatusCheckerTaskTestException extends CloudbreakServiceException {

        SimpleStatusCheckerTaskTestException() {
            super("message");
        }

    }
}