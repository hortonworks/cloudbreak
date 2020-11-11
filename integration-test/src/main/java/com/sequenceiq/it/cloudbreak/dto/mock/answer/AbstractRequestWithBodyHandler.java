package com.sequenceiq.it.cloudbreak.dto.mock.answer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.junit.Assert;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.mock.GenericRequestResponse;
import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;

abstract class AbstractRequestWithBodyHandler<S, T> extends AbstractRequestHandler<T> {
    private Map<String, String> headers = new HashMap<>();

    AbstractRequestWithBodyHandler(Method method, String path, Class<T> requestType, HttpMock mock) {
        super(method, path, requestType, mock);
    }

    public HttpMock thenReturn(GenericRequestResponse<S, T> genericResponse) {
        getMock().getDynamicRouteStack().route(getMethod().getHttpMethod(), getPath(), (request, response, model) -> {
            Map<String, String> uriParameters = request.params();
            T requestBody = prepareRequestInstance(request);
            save(requestBody, uriParameters);

            return genericResponse.handle(requestBody, model, uriParameters);
        });

        return getMock();
    }

    abstract T prepareRequestInstance(spark.Request request);

    public HttpMock verifyRequestBodies(Matcher<Iterable<? super T>> matcher) {
        getMock().then(new Assertion<HttpMock, CloudbreakClient>() {
            @Override
            public HttpMock doAssertion(TestContext testContext, HttpMock testDto, CloudbreakClient client) throws Exception {
                Assert.assertThat(getPath() + " uri " + getMethod().getMethodName() + " method", requestBodies(), matcher);
                return testDto;
            }
        });
        return getMock();
    }

    public AbstractRequestWithBodyHandler thenReturnHeader(String header, String value) {
        headers.put(header, value);
        return this;
    }

    private List<T> requestBodies() {
        return getMock().getRequestList()
                .stream()
                .filter(req -> req.getPath().equals(getPath()) && req.getMethod().equals(getMethod()))
                .map(req -> (T) req.getRequestBody())
                .collect(Collectors.toList());
    }
}
