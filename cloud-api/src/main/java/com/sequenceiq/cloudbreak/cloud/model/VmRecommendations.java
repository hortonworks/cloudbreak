package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VmRecommendations {

    private VmRecommendation master;

    private VmRecommendation worker;

    public VmRecommendation getMaster() {
        return master;
    }

    public void setMaster(VmRecommendation master) {
        this.master = master;
    }

    public VmRecommendation getWorker() {
        return worker;
    }

    public void setWorker(VmRecommendation worker) {
        this.worker = worker;
    }
}
