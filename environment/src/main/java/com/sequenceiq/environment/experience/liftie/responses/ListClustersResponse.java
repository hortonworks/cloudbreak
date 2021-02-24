package com.sequenceiq.environment.experience.liftie.responses;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListClustersResponse {

    private Map<String, LiftieClusterView> clusters;

    private PageStats page;

    public Map<String, LiftieClusterView> getClusters() {
        return clusters;
    }

    public void setClusters(Map<String, LiftieClusterView> clusters) {
        this.clusters = clusters;
    }

    public PageStats getPage() {
        return page;
    }

    public void setPage(PageStats page) {
        this.page = page;
    }

}
