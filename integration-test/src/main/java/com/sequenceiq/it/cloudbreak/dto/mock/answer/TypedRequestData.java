package com.sequenceiq.it.cloudbreak.dto.mock.answer;

import java.util.Map;

import com.sequenceiq.it.cloudbreak.dto.mock.Method;

public class TypedRequestData<T> {
    private final String path;

    private final long time;

    private T requestBody;

    private Map<String, String> uriParameters;

    private Method method;

    public TypedRequestData(T requestBody, Map<String, String> uriParameters, String path, Method method) {
        this.requestBody = requestBody;
        this.uriParameters = uriParameters;
        this.path = path;
        time = System.currentTimeMillis();
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public T getRequestBody() {
        return requestBody;
    }

    public long getTime() {
        return time;
    }

    public Map<String, String> getUriParameters() {
        return uriParameters;
    }

    public Method getMethod() {
        return method;
    }
}
