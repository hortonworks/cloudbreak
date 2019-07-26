package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import io.swagger.annotations.ApiModel;

@ApiModel
public class CheckRightV4SingleResponse {

    private String right;

    private Boolean result;

    public CheckRightV4SingleResponse() {
    }

    public CheckRightV4SingleResponse(String right, Boolean result) {
        this.right = right;
        this.result = result;
    }

    public String getRight() {
        return right;
    }

    public void setRight(String right) {
        this.right = right;
    }

    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }
}
