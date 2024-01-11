package com.sequenceiq.mock.config;

import java.util.List;

public class BadClustersConfig {

    private Integer numBadClusters;

    private List<String> urisForLongDelay;

    private String longDelayInSecs;

    public Integer getNumBadClusters() {
        return numBadClusters;
    }

    public void setNumBadClusters(Integer numBadClusters) {
        this.numBadClusters = numBadClusters;
    }

    public List<String> getUrisForLongDelay() {
        return urisForLongDelay;
    }

    public void setUrisForLongDelay(List<String> urisForLongDelay) {
        this.urisForLongDelay = urisForLongDelay;
    }

    public String getLongDelayInSecs() {
        return longDelayInSecs;
    }

    public void setLongDelayInSecs(String longDelayInSecs) {
        this.longDelayInSecs = longDelayInSecs;
    }

}
