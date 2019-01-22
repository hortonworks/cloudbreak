package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupParametersV4Base;

public class GcpInstanceGroupParametersV4 extends InstanceGroupParametersV4Base {

    private String opId;

    public String getOpId() {
        return opId;
    }

    public void setOpId(String opId) {
        this.opId = opId;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        map.put("opid", opId);
        return map;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        super.parse(parameters);
        setDiscoveryName(getParameterOrNull(parameters, "discoveryName"));
        setInstanceName(getParameterOrNull(parameters, "instanceName"));
        opId = getParameterOrNull(parameters, "opid");
    }
}
