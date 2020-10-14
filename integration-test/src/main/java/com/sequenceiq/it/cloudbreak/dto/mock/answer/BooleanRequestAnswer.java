package com.sequenceiq.it.cloudbreak.dto.mock.answer;

import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;

import spark.Request;

public class BooleanRequestAnswer<S> extends AbstractRequestWithBodyHandler<S, Boolean, BooleanRequestAnswer<S>> {
    public BooleanRequestAnswer(Method method, String path, Class<Boolean> requestType, HttpMock mock, ExecuteQueryToMockInfrastructure executeQuery) {
        super(method, path, requestType, mock, executeQuery);
    }

    @Override
    Boolean prepareRequestInstance(Request request) {
        return Boolean.valueOf(request.body());
    }
}
