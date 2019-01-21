package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack;

import java.util.Map;

public class AzureStackParametersV4 extends StackParameterV4Base {

    @Override
    public <T> T toClass(Map<String, Object> parameters) {
        return (T) this;
    }
}
