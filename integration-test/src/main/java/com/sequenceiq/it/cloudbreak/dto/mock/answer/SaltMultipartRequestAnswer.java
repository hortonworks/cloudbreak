package com.sequenceiq.it.cloudbreak.dto.mock.answer;

import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;

import spark.Request;

public class SaltMultipartRequestAnswer<S> extends AbstractRequestWithBodyHandler<S, SaltFile> {
    public SaltMultipartRequestAnswer(Method method, String path, Class<String> requestType, HttpMock mock) {
        super(method, path, SaltFile.class, mock);
    }

    @Override
    SaltFile prepareRequestInstance(Request request) {
        return SaltFile.create(request.raw());
    }
}
