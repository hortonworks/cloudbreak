package com.sequenceiq.cloudbreak.cm.polling.task;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;

import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;

public class Http500Answer implements Answer<ApiCommand> {

    //CHECKSTYLE:OFF
    private int invocationCounter = 0;
    //CHECKSTYLE:ON

    private final int http500Limit;

    public Http500Answer(int http500Limit) {
        this.http500Limit = http500Limit;
    }

    @Override
    public ApiCommand answer(InvocationOnMock invocation) throws Throwable {
        if (invocationCounter++ < http500Limit) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "error");
        } else {
            ApiCommand apiCommand = new ApiCommand();
            apiCommand.setActive(false);
            apiCommand.setSuccess(true);
            return apiCommand;
        }
    }
}
