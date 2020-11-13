package com.sequenceiq.it.cloudbreak.dto.mock.answer;

import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;

public class StringRequestAnswer<S> extends AbstractRequestWithBodyHandler<S, String, StringRequestAnswer<S>> {
    public StringRequestAnswer(Method method, String path, Class<String> requestType, HttpMock mock, ExecuteQueryToMockInfrastructure executeQuery) {
        super(method, path, requestType, mock, executeQuery);
    }
//
//    @Override
//    String prepareRequestInstance(Request request) {
//        return request.body();
//    }
}
