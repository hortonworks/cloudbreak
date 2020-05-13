package com.sequenceiq.environment.environment.experience.liftie.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "ClusterViews")
public class ClusterViews {

    List<ClusterView> clusters;

    public List<ClusterView> getClusters() {
        return clusters;
    }

    public void setClusters(List<ClusterView> clusters) {
        this.clusters = clusters;
    }

}
