package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;

public abstract class StackParameterV4Base implements JsonEntity, Mappable {
    @Override
    public Map<String, Object> asMap() {
        return new HashMap<>();
    }
}
