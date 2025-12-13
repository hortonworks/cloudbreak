package com.sequenceiq.cloudbreak.cm.polling;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

class PollingResultErrorHandlerTest {

    private static final String CANCELLATION_MESSAGE = "Poller exited with error";

    private static final String TIMEOUT_MESSAGE = "Poller exited with timeout";

    private final PollingResultErrorHandler underTest = new PollingResultErrorHandler();

    @Test
    void testHandlePollingResultShouldNotDoAnythingWhenTheResultIsSuccess() throws CloudbreakException {
        underTest.handlePollingResult(PollingResult.SUCCESS, CANCELLATION_MESSAGE, TIMEOUT_MESSAGE);
    }

}