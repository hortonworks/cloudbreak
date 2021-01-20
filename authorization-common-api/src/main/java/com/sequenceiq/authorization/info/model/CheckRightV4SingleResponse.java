package com.sequenceiq.authorization.info.model;

import io.swagger.annotations.ApiModel;

@ApiModel
public class CheckRightV4SingleResponse {

    private RightV4 right;

    private Boolean result;

    public CheckRightV4SingleResponse() {
    }

    public CheckRightV4SingleResponse(RightV4 right, Boolean result) {
        this.right = right;
        this.result = result;
    }

    public RightV4 getRight() {
        return right;
    }

    public void setRight(RightV4 right) {
        this.right = right;
    }

    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }
}
