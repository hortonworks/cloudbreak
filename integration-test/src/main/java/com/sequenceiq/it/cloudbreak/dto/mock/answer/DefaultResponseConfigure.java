package com.sequenceiq.it.cloudbreak.dto.mock.answer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;

import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;
import com.sequenceiq.it.cloudbreak.testcase.mock.response.MockResponse;

public class DefaultResponseConfigure<T extends CloudbreakTestDto> {
    private final Method method;

    private final String path;

    private final T testDto;

    private final ExecuteQueryToMockInfrastructure executeQuery;

    private final Map<String, String> pathVariables = new HashMap<>();

    public DefaultResponseConfigure(Method method, String path, T testDto, ExecuteQueryToMockInfrastructure executeQuery) {
        this.method = method;
        this.path = path;
        this.executeQuery = executeQuery;
        this.testDto = testDto;
    }

    public Method getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public T clearDefinedResponses() {
//        mock.getDynamicRouteStack().clear(method.getHttpMethod(), path);
//        executeQuery.executeConfigure("/tests/clear", pathVariables, new MockResponse(null, "GET", path, times, 200));
        return testDto;
    }

    public T clearCalls() {
//        mock.getRequestList().clear();
        return testDto;
    }

    public T verifyRequestsParameters(Matcher<? super List<Map<String, String>>> matcher) {
//        mock.then((testContext, testDto, client) -> {
//            Assert.assertThat(path + " uri " + method + " method", requestParameters(), matcher);
//            return testDto;
//        });
        return testDto;
    }

    public DefaultResponseConfigure<T>  atLeast(int atLeast) {
//        return verify(CheckCount.atLeast(atLeast));
        return this;
    }

    public DefaultResponseConfigure<T> times(int times) {
//        return verify(CheckCount.times(times));
        return this;
    }

    public T verify() {
//        testDto.then((testContext, testDto, client) -> {
//            verification.handle(path, method, client, requests());
//            return testDto;
//        });
        return testDto;
    }

    public DefaultResponseConfigure<T> bodyContains(String body, int times) {
//        getMock().then((testContext, testDto, client) -> {
//            Assert.assertThat(getPath() + " uri " + getMethod().getMethodName() + " method", requestBodies(), matcher);
//            return testDto;
//        });
        return this;
    }

    protected void pathVariableInternal(String name, String value) {
        pathVariables.put(name, value);
    }

    protected Map<String, String> pathVariables() {
        return pathVariables;
    }

    public T thenReturn(String message, int statusCode) {
        return thenReturn(message, statusCode, 1);
    }

    public T thenReturn(String message, int statusCode, int times) {
        return thenReturn(null, message, statusCode, times);
    }

    public T thenReturn(Object retValue, String message, int statusCode, int times) {
        String crn = testDto.getCrn();
        if (crn != null) {
            pathVariable("mockUuid", crn);
        }
        executeQuery.executeConfigure(pathVariables(), new MockResponse(retValue, message, getMethod().getHttpMethod().name(), getPath(), times, statusCode));
        return testDto;
    }

    public T thenReturn(Object retValue) {
        return thenReturn(retValue, null, 200, 1);
    }

    public DefaultResponseConfigure<T> pathVariable(String name, String value) {
        pathVariableInternal(name, value);
        return this;
    }
}
