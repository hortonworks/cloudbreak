package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureInstanceGroupV4Parameters extends InstanceGroupV4ParametersBase {

    @Schema
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
        if (availabilitySet != null) {
            putIfValueNotNull(map, "faultDomainCount", availabilitySet.getFaultDomainCount());
            putIfValueNotNull(map, "name", availabilitySet.getName());
            putIfValueNotNull(map, "updateDomainCount", availabilitySet.getUpdateDomainCount());
        }
        return map;
    }

    @Override
    @JsonIgnore
    @Schema(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        super.parse(parameters);
        AzureAvailabiltySetV4 availabiltySet = new AzureAvailabiltySetV4();
        availabiltySet.setFaultDomainCount(getInt(parameters, "faultDomainCount"));
        availabiltySet.setName(getParameterOrNull(parameters, "name"));
        availabiltySet.setUpdateDomainCount(getInt(parameters, "updateDomainCount"));
        if (availabiltySet.getFaultDomainCount() != null
                || availabiltySet.getName() != null
                || availabiltySet.getUpdateDomainCount() != null) {
            availabilitySet = availabiltySet;
        }
    }
}
