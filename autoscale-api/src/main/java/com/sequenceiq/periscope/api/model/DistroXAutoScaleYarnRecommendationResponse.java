package com.sequenceiq.periscope.api.model;

import java.util.List;

import com.sequenceiq.periscope.doc.ApiDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DistroXAutoScaleYarnRecommendationResponse implements Json {

    @ApiModelProperty(ApiDescription.ClusterJsonsProperties.YARN_RECOMMENDATION)
    private List<String> decommissionNodeIds;

    public List<String> getDecommissionNodeIds() {
        return decommissionNodeIds;
    }

    public void setDecommissionNodeIds(List<String> decommissionNodeIds) {
        this.decommissionNodeIds = decommissionNodeIds;
    }
}
