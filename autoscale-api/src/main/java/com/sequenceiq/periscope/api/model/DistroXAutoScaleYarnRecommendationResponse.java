package com.sequenceiq.periscope.api.model;

import java.util.List;

import com.sequenceiq.periscope.doc.ApiDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class DistroXAutoScaleYarnRecommendationResponse implements Json {

    @Schema(description = ApiDescription.ClusterJsonsProperties.YARN_RECOMMENDATION)
    private List<String> decommissionNodeIds;

    public List<String> getDecommissionNodeIds() {
        return decommissionNodeIds;
    }

    public void setDecommissionNodeIds(List<String> decommissionNodeIds) {
        this.decommissionNodeIds = decommissionNodeIds;
    }
}
