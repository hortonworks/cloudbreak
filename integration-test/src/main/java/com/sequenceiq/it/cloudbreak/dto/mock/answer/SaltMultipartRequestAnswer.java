package com.sequenceiq.it.cloudbreak.dto.mock.answer;

import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;

import spark.Request;

public class SaltMultipartRequestAnswer<S> extends AbstractRequestWithBodyHandler<S, SaltFile, SaltMultipartRequestAnswer<S>> {
    public SaltMultipartRequestAnswer(Method method, String path, Class<String> requestType, HttpMock mock, ExecuteQueryToMockInfrastructure executeQuery) {
        super(method, path, SaltFile.class, mock, executeQuery);
    }

    @Override
    SaltFile prepareRequestInstance(Request request) {
        return SaltFile.create(request.raw());
    }
}
