package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack;

import java.util.Map;

public class OpenStackStackParametersV4 extends StackParameterV4Base {

    @Override
    public <T> T toClass(Map<String, Object> parameters) {
        OpenStackStackParametersV4 ret = new OpenStackStackParametersV4();
        ret.setTimeToLive(getTimeToLive(parameters));
        return (T) ret;
    }
}
