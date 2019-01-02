package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;

public abstract class MappableBase implements Mappable {

    @Override
    public Map<String, Object> asMap() {
        return defaultMap();
    }
}
