package com.sequenceiq.it.cloudbreak.dto.mock.answer;


import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.it.cloudbreak.dto.mock.GenericResponse;
import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;

public class AnswerWithoutRequest<S> extends AbstractRequestHandler {

    private Map<String, String> headers = new HashMap<>();

    public AnswerWithoutRequest(Method method, String path, Class requestType, HttpMock mock) {
        super(method, path, String.class, mock);
    }

    public HttpMock thenReturn(GenericResponse<S> genericResponse) {
        getMock().getDynamicRouteStack().route(getMethod().getHttpMethod(), getPath(), (request, response, model) -> {
            Map<String, String> uriParameters = request.params();
            save(request.body(), uriParameters);

            headers.forEach((header, value) -> response.header(header, value));
            return genericResponse.handle(model, uriParameters);
        });

        return getMock();
    }

    public AnswerWithoutRequest thenReturnHeader(String header, String value) {
        headers.put(header, value);
        return this;
    }

}
