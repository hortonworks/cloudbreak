package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VmRecommendations {

    private VmRecommendation master;

    private VmRecommendation worker;

    private VmRecommendation broker;

    private VmRecommendation quorum;

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

    public VmRecommendation getBroker() {
        return broker;
    }

    public void setBroker(VmRecommendation broker) {
        this.broker = broker;
    }

    public VmRecommendation getQuorum() {
        return quorum;
    }

    public void setQuorum(VmRecommendation quorum) {
        this.quorum = quorum;
    }
}
