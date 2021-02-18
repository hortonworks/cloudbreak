package com.sequenceiq.mock.experience.response.liftie;

import java.util.Map;

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
