package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;

import io.swagger.annotations.ApiModelProperty;

public abstract class StackParameterV4Base implements JsonEntity, Mappable {

    @ApiModelProperty
    private Long timeToLive;

    public Long getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(Long timeToLive) {
        this.timeToLive = timeToLive;
    }

    protected Long getTimeToLive(Map<String, Object> parameters) {
        String timeToLive = getParameterOrNull(parameters, "timetolive");
        if (timeToLive != null) {
            return Long.parseLong(timeToLive);
        }
        return null;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("timetolive", timeToLive);
        return map;
    }
}
