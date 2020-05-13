package com.sequenceiq.environment.environment.experience.liftie.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "ListClustersResponse")
public class ListClustersResponse {

    private ClusterViews clusters;

    private PageStats page;

    public ClusterViews getClusters() {
        return clusters;
    }

    public void setClusters(ClusterViews clusters) {
        this.clusters = clusters;
    }

    public PageStats getPage() {
        return page;
    }

    public void setPage(PageStats page) {
        this.page = page;
    }

}
