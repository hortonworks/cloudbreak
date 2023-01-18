package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkScaleV4Request implements JsonEntity {

    @Schema(description = ModelDescriptions.InstanceGroupNetworkScaleModelDescription.PREFERRED_SUBNET_IDS)
    private List<String> preferredSubnetIds = new ArrayList<>();

    @Schema(description = ModelDescriptions.InstanceGroupNetworkScaleModelDescription.PREFERRED_AZS)
    private Set<String> preferredAvailabilityZones = new HashSet<>();

    public List<String> getPreferredSubnetIds() {
        return preferredSubnetIds;
    }

    public void setPreferredSubnetIds(List<String> preferredSubnetIds) {
        this.preferredSubnetIds = preferredSubnetIds;
    }

    public Set<String> getPreferredAvailabilityZones() {
        return preferredAvailabilityZones;
    }

    public void setPreferredAvailabilityZones(Set<String> preferredAvailabilityZones) {
        this.preferredAvailabilityZones = preferredAvailabilityZones;
    }
}
