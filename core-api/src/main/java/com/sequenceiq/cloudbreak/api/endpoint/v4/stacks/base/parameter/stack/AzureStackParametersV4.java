package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

public class AzureStackParametersV4 implements JsonEntity, Mappable {
    @Override
    public Map<String, Object> asMap() {
        return null;
    }

    @Override
    public <T> T toClass(Map<String, Object> parameters) {
        return null;
    }
}
