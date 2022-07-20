package com.sequenceiq.mock.spi;

public class MockResponse {

    private Object response;

    private String message;

    private String httpMethod;

    private String path;

    private int times;

    private int statusCode;

    private String clss;

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

    @Override
    public String toString() {
        return "MockResponse{" +
                "response=" + response +
                ", message='" + message + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", path='" + path + '\'' +
                ", times=" + times +
                ", statusCode=" + statusCode +
                ", clss='" + clss + '\'' +
                '}';
    }
}
