package com.sequenceiq.distrox.api.v1.distrox.model.network;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkScaleV1Request implements JsonEntity {

    @Schema(description = ModelDescriptions.InstanceGroupNetworkScaleModelDescription.PREFERRED_SUBNET_IDS)
    private List<String> preferredSubnetIds = new ArrayList<>();

    public List<String> getPreferredSubnetIds() {
        return preferredSubnetIds;
    }

    public void setPreferredSubnetIds(List<String> preferredSubnetIds) {
        this.preferredSubnetIds = preferredSubnetIds;
    }
}
