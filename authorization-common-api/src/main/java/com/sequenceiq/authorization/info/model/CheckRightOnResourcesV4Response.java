package com.sequenceiq.authorization.info.model;

import java.util.List;

import io.swagger.annotations.ApiModel;

@ApiModel
public class CheckRightOnResourcesV4Response {

    private RightV4 right;

    private List<CheckResourceRightV4Response> responses;

    public RightV4 getRight() {
        return right;
    }

    public void setRight(RightV4 right) {
        this.right = right;
    }

    public List<CheckResourceRightV4Response> getResponses() {
        return responses;
    }

    public void setResponses(List<CheckResourceRightV4Response> responses) {
        this.responses = responses;
    }
}
