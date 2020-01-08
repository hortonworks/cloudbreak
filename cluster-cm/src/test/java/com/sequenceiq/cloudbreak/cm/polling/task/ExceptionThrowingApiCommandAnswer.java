package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.cloudera.api.swagger.model.ApiCommand;

public class ExceptionThrowingApiCommandAnswer implements Answer<ApiCommand> {

    private final Queue<Exception> exceptionQueue = new LinkedList<>();

    public ExceptionThrowingApiCommandAnswer(Exception... exceptions) {
        exceptionQueue.addAll(Arrays.asList(exceptions));
    }

    @Override
    public ApiCommand answer(InvocationOnMock invocation) throws Throwable {
        Exception exception = exceptionQueue.poll();
        if (exception != null) {
            throw exception;
        }
        ApiCommand apiCommand = new ApiCommand();
        apiCommand.setActive(false);
        apiCommand.setSuccess(true);
        return apiCommand;
    }
}
