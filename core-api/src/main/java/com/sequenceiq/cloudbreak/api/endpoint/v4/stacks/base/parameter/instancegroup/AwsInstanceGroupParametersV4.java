package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupParametersV4Base;

public class AwsInstanceGroupParametersV4 extends InstanceGroupParametersV4Base {

    @Override
    public <T> T toClass(Map<String, Object> parameters) {
        AwsInstanceGroupParametersV4 ret = new AwsInstanceGroupParametersV4();
        ret.setDiscoveryName(getParameterOrNull(parameters, "discoveryName"));
        ret.setInstanceName(getParameterOrNull(parameters, "instanceName"));
        return (T) ret;
    }
}
