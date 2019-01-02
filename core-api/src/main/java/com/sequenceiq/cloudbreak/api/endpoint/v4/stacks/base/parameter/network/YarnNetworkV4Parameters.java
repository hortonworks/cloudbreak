package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;

public class YarnNetworkV4Parameters implements JsonEntity, Mappable {

    @Override
    public Map<String, Object> asMap() {
        return new HashMap<>();
    }
}
