package com.sequenceiq.it.cloudbreak.dto.mock.answer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.junit.Assert;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.dto.mock.Verification;

abstract class AbstractRequestHandler<T> {
    private final Method method;

    private final String path;

    private final HttpMock mock;

    private final Class<T> requestType;

    AbstractRequestHandler(Method method, String path, Class<T> requestType, HttpMock mock) {
        this.method = method;
        this.path = path;
        this.mock = mock;
        this.requestType = requestType;
    }

    public Method getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public HttpMock getMock() {
        return mock;
    }

    public Class<T> getRequestType() {
        return requestType;
    }

    public HttpMock clearDefinedResponses() {
        mock.getDynamicRouteStack().clear(method.getHttpMethod(), path);

        return mock;
    }

    public HttpMock verifyRequestsParameters(Matcher<? super List<Map<String, String>>> matcher) {
        mock.then(new Assertion<HttpMock, CloudbreakClient>() {
            @Override
            public HttpMock doAssertion(TestContext testContext, HttpMock testDto, CloudbreakClient client) throws Exception {
                Assert.assertThat(path + " uri " + method + " method", requestParameters(), matcher);
                return testDto;
            }
        });
        return mock;
    }

    public HttpMock verify(Verification verification) {
        getMock().then(new Assertion<HttpMock, CloudbreakClient>() {
            @Override
            public HttpMock doAssertion(TestContext testContext, HttpMock testDto, CloudbreakClient client) throws Exception {
                verification.handle(path, method, client, getMock().getModel(), requests());
                return testDto;
            }
        });
        return getMock();
    }

    protected List<Map<String, String>> requestParameters() {
        return mock.getRequestList()
                .stream()
                .filter(req -> req.getPath().equals(path) && req.getMethod().equals(method))
                .map(req -> req.getUriParameters())
                .collect(Collectors.toList());
    }

    protected List<TypedRequestData<T>> requests() {
        return getMock().getRequestList()
                .stream()
                .filter(req -> req.getPath().equals(getPath()) && req.getMethod().equals(getMethod()))
                .map(req -> new TypedRequestData<>((T) req.getRequestBody(), req.getUriParameters(), req.getPath(), req.getMethod()))
                .collect(Collectors.toList());
    }

    void save(T requestBody, Map<String, String> uriParameters) {
        mock.getRequestList().add(new RequestData(requestBody, uriParameters, path, method));
    }

    public String getFullUrl() {
        return getMock().getSparkServer().getEndpoint() + getPath();
    }
}
