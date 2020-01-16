package com.sequenceiq.it.cloudbreak.dto.mock.answer;

import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;

import spark.Request;

public class StringRequestAnswer<S> extends AbstractRequestWithBodyHandler<S, String> {
    public StringRequestAnswer(Method method, String path, Class<String> requestType, HttpMock mock) {
        super(method, path, requestType, mock);
    }

    @Override
    String prepareRequestInstance(Request request) {
        return request.body();
    }
}
