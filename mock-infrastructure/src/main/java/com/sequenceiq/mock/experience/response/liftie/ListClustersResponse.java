package com.sequenceiq.mock.experience.response.liftie;

import java.util.Map;

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
