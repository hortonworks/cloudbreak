package com.sequenceiq.it.cloudbreak.dto.mock.answer;


import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.it.cloudbreak.dto.mock.GenericResponse;
import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;
import com.sequenceiq.it.cloudbreak.testcase.mock.response.MockResponse;

public class AnswerWithoutRequest<S> extends AbstractRequestHandler {

    private Map<String, String> headers = new HashMap<>();

    public AnswerWithoutRequest(Method method, String path, Class requestType, HttpMock mock, ExecuteQueryToMockInfrastructure executeQuery) {
        super(method, path, String.class, mock, executeQuery);
    }

    public HttpMock thenReturn(GenericResponse<S> genericResponse) {
        S handle = genericResponse.handle(null);
        executeQuery().executeConfigure(getPath(), pathVariables(), new MockResponse(handle, getMethod().getHttpMethod().name(), getPath()));
        return getMock();
    }

    public AnswerWithoutRequest thenReturnHeader(String header, String value) {
        headers.put(header, value);
        return this;
    }

}
