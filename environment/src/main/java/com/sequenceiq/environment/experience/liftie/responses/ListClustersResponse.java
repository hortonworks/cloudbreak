package com.sequenceiq.environment.experience.liftie.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "ListClustersResponse")
public class ListClustersResponse {

    private Map<String, ClusterView> clusters;

    private PageStats page;

    public Map<String, ClusterView> getClusters() {
        return clusters;
    }

    public void setClusters(Map<String, ClusterView> clusters) {
        this.clusters = clusters;
    }

    public PageStats getPage() {
        return page;
    }

    public void setPage(PageStats page) {
        this.page = page;
    }

}
