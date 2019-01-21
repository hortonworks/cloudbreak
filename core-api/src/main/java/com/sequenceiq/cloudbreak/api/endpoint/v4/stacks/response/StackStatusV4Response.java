package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import java.util.Map;

import io.swagger.annotations.ApiModelProperty;

public class StackStatusV4Response {

    @ApiModelProperty
    private Map<String, Object> statuses;

    public Map<String, Object> getStatuses() {
        return statuses;
    }

    public void setStatuses(Map<String, Object> statuses) {
        this.statuses = statuses;
    }
}
