package com.sequenceiq.authorization.info.model;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class CheckRightOnResourcesV4Response {

    private RightV4 right;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<CheckResourceRightV4Response> responses = new ArrayList<>();

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
