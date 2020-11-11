package com.sequenceiq.it.cloudbreak.testcase.mock.response;

public class MockResponse {

    private Object response;

    private String httpMethod;

    private String path;

    public MockResponse(Object response, String httpMethod, String path) {
        this.response = response;
        this.httpMethod = httpMethod;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
}
