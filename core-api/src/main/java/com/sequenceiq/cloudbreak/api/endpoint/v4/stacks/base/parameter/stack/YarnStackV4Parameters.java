package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack;

import java.util.Map;

import io.swagger.annotations.ApiModelProperty;

public class YarnStackV4Parameters extends StackV4ParameterBase {

    @ApiModelProperty
    private boolean yarnQueue;

    public boolean isYarnQueue() {
        return yarnQueue;
    }

    public void setYarnQueue(boolean yarnQueue) {
        this.yarnQueue = yarnQueue;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> ret = super.asMap();
        ret.put("yarnQueue", yarnQueue);
        return ret;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        super.parse(parameters);
        yarnQueue = getBoolean(parameters, "yarnQueue");
    }
}
