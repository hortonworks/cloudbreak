package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupParametersV4Base;

import io.swagger.annotations.ApiModelProperty;

public class AzureInstanceGroupParametersV4 extends InstanceGroupParametersV4Base {

    @ApiModelProperty
    private AzureAvailabiltySetV4 availabilitySet;

    public AzureAvailabiltySetV4 getAvailabilitySet() {
        return availabilitySet;
    }

    public void setAvailabilitySet(AzureAvailabiltySetV4 availabilitySet) {
        this.availabilitySet = availabilitySet;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        map.put("faultDomainCount", availabilitySet.getFaultDomainCount());
        map.put("name", availabilitySet.getName());
        map.put("updateDomainCount", availabilitySet.getUpdateDomainCount());
        return map;
    }

    @Override
    public <T> T toClass(Map<String, Object> parameters) {
        setDiscoveryName(getParameterOrNull(parameters, "discoveryName"));
        setInstanceName(getParameterOrNull(parameters, "instanceName"));
        AzureAvailabiltySetV4 availabiltySet = new AzureAvailabiltySetV4();
        availabiltySet.setFaultDomainCount(getParameterOrNull(parameters, "faultDomainCount"));
        availabiltySet.setName(getParameterOrNull(parameters, "name"));
        availabiltySet.setUpdateDomainCount(getParameterOrNull(parameters, "updateDomainCount"));
        availabilitySet = availabiltySet;
        return (T) this;
    }
}
