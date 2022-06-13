package com.sequenceiq.cloudbreak.cloud.yarn.client.model.response;

public class ResponseContext {

    private int statusCode;

    private ApplicationResponse responseObject;

    private ApplicationErrorResponse responseError;

    public ApplicationResponse getResponseObject() {
        return responseObject;
    }

    public void setResponseObject(ApplicationResponse responseObject) {
        this.responseObject = responseObject;
    }

    public ApplicationErrorResponse getResponseError() {
        return responseError;
    }

    public void setResponseError(ApplicationErrorResponse responseError) {
        this.responseError = responseError;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public String toString() {
        return "ResponseContext{" +
                "statusCode=" + statusCode +
                ", responseObject=" + responseObject +
                ", responseError=" + responseError +
                '}';
    }
}
