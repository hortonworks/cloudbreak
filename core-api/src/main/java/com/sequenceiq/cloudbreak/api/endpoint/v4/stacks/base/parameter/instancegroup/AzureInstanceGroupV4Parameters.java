package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupV4ParametersBase;

import io.swagger.annotations.ApiModelProperty;

public class AzureInstanceGroupV4Parameters extends InstanceGroupV4ParametersBase {

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
    public void parse(Map<String, Object> parameters) {
        super.parse(parameters);
        AzureAvailabiltySetV4 availabiltySet = new AzureAvailabiltySetV4();
        availabiltySet.setFaultDomainCount(getParameterOrNull(parameters, "faultDomainCount"));
        availabiltySet.setName(getParameterOrNull(parameters, "name"));
        availabiltySet.setUpdateDomainCount(getParameterOrNull(parameters, "updateDomainCount"));
        availabilitySet = availabiltySet;
    }
}
