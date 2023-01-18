package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup;

import java.util.HashMap;
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
            Map<String, Object> availabilitySetMap = new HashMap<>();
            putIfValueNotNull(availabilitySetMap, "faultDomainCount", availabilitySet.getFaultDomainCount());
            putIfValueNotNull(availabilitySetMap, "name", availabilitySet.getName());
            putIfValueNotNull(availabilitySetMap, "updateDomainCount", availabilitySet.getUpdateDomainCount());
            putIfValueNotNull(map, "availabilitySet", availabilitySetMap);
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
        Map<String, Object> availabilitySetMap = null != parameters ?
                (Map<String, Object>) parameters.getOrDefault("availabilitySet", new HashMap<>()) : new HashMap<>();
        AzureAvailabiltySetV4 availabilitySet = new AzureAvailabiltySetV4();
        availabilitySet.setFaultDomainCount(getInt(availabilitySetMap, "faultDomainCount"));
        availabilitySet.setName(getParameterOrNull(availabilitySetMap, "name"));
        availabilitySet.setUpdateDomainCount(getInt(availabilitySetMap, "updateDomainCount"));
        if (availabilitySet.getFaultDomainCount() != null
                || availabilitySet.getName() != null
                || availabilitySet.getUpdateDomainCount() != null) {
            this.availabilitySet = availabilitySet;
        }
    }
}
