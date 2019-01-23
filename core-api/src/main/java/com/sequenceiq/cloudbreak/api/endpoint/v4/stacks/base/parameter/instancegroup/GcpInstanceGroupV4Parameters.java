package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupV4ParametersBase;

public class GcpInstanceGroupV4Parameters extends InstanceGroupV4ParametersBase {

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
        opId = getParameterOrNull(parameters, "opid");
    }
}
