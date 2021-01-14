package com.sequenceiq.it.cloudbreak.dto.mock.answer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.CheckCount;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.dto.mock.Verification;
import com.sequenceiq.it.cloudbreak.dto.mock.verification.TextBodyContainsVerification;
import com.sequenceiq.it.cloudbreak.dto.mock.verification.VerificationContext;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;
import com.sequenceiq.it.cloudbreak.testcase.mock.response.MockResponse;
import com.sequenceiq.it.verification.Call;

public class DefaultResponseConfigure<T extends CloudbreakTestDto> {
    private final Method method;

    private final String path;

    private final T testDto;

    private final ExecuteQueryToMockInfrastructure executeQuery;

    private final Map<String, String> pathVariables = new HashMap<>();

    private final List<Verification> verifications = new LinkedList<>();

    private boolean crnless;

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

    public DefaultResponseConfigure<T> atLeast(int atLeast) {
        verifications.add(CheckCount.atLeast(atLeast));
        return this;
    }

    public DefaultResponseConfigure<T> times(int times) {
        verifications.add(CheckCount.times(times));
        return this;
    }

    public T verify() {
        Call[] calls;
        if (crnless) {
            calls = executeQuery.execute("/tests/calls", w -> w.queryParam("path", path), r -> r.readEntity(Call[].class));
        } else {
            calls = executeQuery.execute("/tests/calls/" + testDto.getCrn(), r -> r.readEntity(Call[].class));
        }
        if (calls == null) {
            calls = new Call[0];
        }
        List<Call> collect = Arrays.stream(calls)
                .filter(this::isPathMatched)
                .filter(call -> call.getMethod().equalsIgnoreCase(method.toString()))
                .collect(Collectors.toList());
        VerificationContext verificationContext = new VerificationContext(collect);
        verifications.forEach(v -> {
            v.handle(path, method, verificationContext);
        });
        List<String> errors = verificationContext.getErrors();
        if (!errors.isEmpty()) {
            throw new TestFailException("URL verification failed: " + System.lineSeparator() + String.join(System.lineSeparator(), errors));
        }
        return testDto;
    }

    public DefaultResponseConfigure<T> bodyContains(String body, int times) {
        verifications.add(new TextBodyContainsVerification(body, times));
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

    private boolean isPathMatched(Call call) {
        String replace = path;
        if (testDto.getCrn() != null) {
            replace = path.replace("{mockUuid}", testDto.getCrn());
        }
        for (Map.Entry<String, String> variable : pathVariables.entrySet()) {
            replace = replace.replace("{" + variable.getKey() + "}", variable.getValue());
        }
        return call.getUri().contains(replace);
    }

    public DefaultResponseConfigure<T> crnless() {
        this.crnless = true;
        return this;
    }
}
