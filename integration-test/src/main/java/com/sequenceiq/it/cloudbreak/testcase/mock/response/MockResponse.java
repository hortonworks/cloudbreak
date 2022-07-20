package com.sequenceiq.it.cloudbreak.testcase.mock.response;

import java.util.Set;

public class MockResponse {

    private Object response;

    private String message;

    private String httpMethod;

    private String path;

    private int times;

    private int statusCode;

    private String clss;
    private Set<String> bodyContains;

    public MockResponse(Object response, String httpMethod, String path, String clss) {
        this(response, null, httpMethod, path, 0, 200, clss, null);
    }

    public MockResponse(Object response, String message, String httpMethod, String path, int times, int statusCode, String clss, Set<String> bodyContains) {
        this.response = response;
        this.message = message;
        this.httpMethod = httpMethod;
        this.path = path;
        this.times = times;
        this.statusCode = statusCode;
        this.clss = clss;
        this.bodyContains = bodyContains;
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

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getClss() {
        return clss;
    }

    public void setClss(String clss) {
        this.clss = clss;
    }

    public Set<String> getBodyContains() {
        return bodyContains;
    }

    public void setBodyContains(Set<String> bodyContains) {
        this.bodyContains = bodyContains;
    }
}
